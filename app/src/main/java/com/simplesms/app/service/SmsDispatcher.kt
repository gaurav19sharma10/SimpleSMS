package com.simplesms.app.service

import android.app.Activity
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.telephony.SmsManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.simplesms.app.data.model.SmsResult
import com.simplesms.app.data.repository.SimManagerHelper
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID
import kotlin.coroutines.resume

object SmsDispatcher {

    private const val TAG = "SmsDispatcher"

    /**
     * Dispatches an SMS message using a specific physical SIM card's subscription ID.
     * Suspends until the baseband radio confirms sending, or until timeout.
     */
    suspend fun sendSms(
        context: Context,
        destinationAddress: String,
        messageText: String,
        subscriptionId: Int,
        timeoutMs: Long = 25000L
    ): SmsResult {
        val result = withTimeoutOrNull(timeoutMs) {
            suspendCancellableCoroutine<SmsResult> { continuation ->
                if (!SimManagerHelper.hasTelephonyPermissions(context)) {
                    continuation.resume(SmsResult.Error("Missing SMS or Phone State permission"))
                    return@suspendCancellableCoroutine
                }

                // 1. Resolve carrier-specific SmsManager instance
                val smsManager = try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        val base = context.getSystemService(SmsManager::class.java)
                        base?.createForSubscriptionId(subscriptionId)
                            ?: @Suppress("DEPRECATION") SmsManager.getSmsManagerForSubscriptionId(subscriptionId)
                    } else {
                        @Suppress("DEPRECATION")
                        SmsManager.getSmsManagerForSubscriptionId(subscriptionId)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Exception resolving SmsManager, falling back", e)
                    @Suppress("DEPRECATION")
                    SmsManager.getSmsManagerForSubscriptionId(subscriptionId)
                } ?: @Suppress("DEPRECATION") SmsManager.getDefault()

                if (smsManager == null) {
                    continuation.resume(SmsResult.Error("SmsManager unavailable for SIM subscription: $subscriptionId"))
                    return@suspendCancellableCoroutine
                }

                // 2. Divide message for carrier limits (160 chars GSM-7 / 70 chars UCS-2)
                val parts = try {
                    smsManager.divideMessage(messageText)
                } catch (e: Exception) {
                    continuation.resume(SmsResult.Error("Failed to format message: ${e.message}"))
                    return@suspendCancellableCoroutine
                }

                val totalParts = parts.size
                val actionId = UUID.randomUUID().toString()
                val sentAction = "com.simplesms.app.SMS_SENT_$actionId"

                // IMPORTANT: For Android 12+ (API 31+), telephony callbacks require FLAG_MUTABLE so the
                // system telephony service (com.android.phone) can populate the result code and extras.
                val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                } else {
                    PendingIntent.FLAG_UPDATE_CURRENT
                }

                val sentIntents = ArrayList<PendingIntent>(totalParts)
                for (i in 0 until totalParts) {
                    // Set explicit package to ensure only this app receives the broadcast
                    val intent = Intent(sentAction).apply {
                        `package` = context.packageName
                        putExtra("part_index", i + 1)
                        putExtra("total_parts", totalParts)
                    }

                    val sentPi = PendingIntent.getBroadcast(
                        context,
                        i,
                        intent,
                        flags
                    )
                    sentIntents.add(sentPi)
                }

                var partsSentSuccess = 0
                var isCleanedUp = false

                val statusReceiver = object : BroadcastReceiver() {
                    override fun onReceive(recvContext: Context?, intent: Intent?) {
                        if (intent?.action != sentAction) return

                        val partNum = intent.getIntExtra("part_index", 1)
                        Log.d(TAG, "SMS part $partNum/$totalParts callback resultCode: $resultCode")

                        when (resultCode) {
                            Activity.RESULT_OK -> {
                                partsSentSuccess++
                                if (partsSentSuccess >= totalParts) {
                                    cleanup()
                                    if (continuation.isActive) {
                                        continuation.resume(
                                            SmsResult.Success(
                                                message = "Sent successfully ($totalParts part${if (totalParts > 1) "s" else ""})",
                                                partsCount = totalParts
                                            )
                                        )
                                    }
                                }
                            }
                            SmsManager.RESULT_ERROR_GENERIC_FAILURE -> {
                                cleanup()
                                if (continuation.isActive) {
                                    continuation.resume(
                                        SmsResult.Error("Carrier rejected SMS (Check SIM balance/recharge)", resultCode)
                                    )
                                }
                            }
                            SmsManager.RESULT_ERROR_NO_SERVICE -> {
                                cleanup()
                                if (continuation.isActive) {
                                    continuation.resume(
                                        SmsResult.Error("No cellular network service", resultCode)
                                    )
                                }
                            }
                            SmsManager.RESULT_ERROR_RADIO_OFF -> {
                                cleanup()
                                if (continuation.isActive) {
                                    continuation.resume(
                                        SmsResult.Error("Airplane mode is active / Cellular radio off", resultCode)
                                    )
                                }
                            }
                            SmsManager.RESULT_ERROR_NULL_PDU -> {
                                cleanup()
                                if (continuation.isActive) {
                                    continuation.resume(
                                        SmsResult.Error("Null PDU error from cellular baseband", resultCode)
                                    )
                                }
                            }
                            else -> {
                                cleanup()
                                if (continuation.isActive) {
                                    continuation.resume(
                                        SmsResult.Error("Carrier error (code: $resultCode)", resultCode)
                                    )
                                }
                            }
                        }
                    }

                    private fun cleanup() {
                        if (!isCleanedUp) {
                            try {
                                context.unregisterReceiver(this)
                            } catch (_: Exception) {}
                            isCleanedUp = true
                        }
                    }
                }

                val filter = IntentFilter(sentAction)

                // CRITICAL FIX: On Android 13+ (API 33+), the broadcast is delivered by the external system
                // process 'com.android.phone'. Therefore, the receiver MUST be registered with RECEIVER_EXPORTED.
                // Because we set `package = context.packageName` on the PendingIntent above, only this app
                // receives the broadcast securely.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    ContextCompat.registerReceiver(
                        context,
                        statusReceiver,
                        filter,
                        ContextCompat.RECEIVER_EXPORTED
                    )
                } else {
                    context.registerReceiver(statusReceiver, filter)
                }

                continuation.invokeOnCancellation {
                    if (!isCleanedUp) {
                        try {
                            context.unregisterReceiver(statusReceiver)
                        } catch (_: Exception) {}
                        isCleanedUp = true
                    }
                }

                // 3. Dispatch to baseband hardware
                try {
                    if (totalParts <= 1) {
                        // For single part SMS, use sendTextMessage directly for rock-solid reliability across all RILs
                        smsManager.sendTextMessage(
                            destinationAddress,
                            null,
                            messageText,
                            sentIntents[0],
                            null // Pass null for deliveryIntent (carriers like Jio don't support delivery reports)
                        )
                    } else {
                        smsManager.sendMultipartTextMessage(
                            destinationAddress,
                            null,
                            parts,
                            sentIntents,
                            null
                        )
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to dispatch SMS through baseband", e)
                    if (!isCleanedUp) {
                        try {
                            context.unregisterReceiver(statusReceiver)
                        } catch (_: Exception) {}
                        isCleanedUp = true
                    }
                    if (continuation.isActive) {
                        continuation.resume(SmsResult.Error("Radio dispatch failed: ${e.localizedMessage ?: e.message}"))
                    }
                }
            }
        }

        return result ?: SmsResult.Error("SMS dispatch timed out (no carrier radio acknowledgement within ${timeoutMs / 1000}s)")
    }
}
