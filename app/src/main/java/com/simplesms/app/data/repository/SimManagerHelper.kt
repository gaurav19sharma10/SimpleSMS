package com.simplesms.app.data.repository

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import androidx.core.content.ContextCompat
import com.simplesms.app.data.model.SimCardInfo

object SimManagerHelper {

    fun hasTelephonyPermissions(context: Context): Boolean {
        val sendSms = ContextCompat.checkSelfPermission(
            context, Manifest.permission.SEND_SMS
        ) == PackageManager.PERMISSION_GRANTED

        val phoneState = ContextCompat.checkSelfPermission(
            context, Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED

        return sendSms && phoneState
    }

    fun hasContactsPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    /**
     * Resolves all active SIM cards inserted into the physical or eSIM slots.
     */
    fun getActiveSimCards(context: Context): List<SimCardInfo> {
        if (!hasTelephonyPermissions(context)) return emptyList()

        val subscriptionManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE)
            as? SubscriptionManager ?: return emptyList()

        val activeList: List<SubscriptionInfo>? = try {
            subscriptionManager.activeSubscriptionInfoList
        } catch (_: SecurityException) {
            emptyList()
        }

        val hasReadPhoneNumbers = ContextCompat.checkSelfPermission(
            context, Manifest.permission.READ_PHONE_NUMBERS
        ) == PackageManager.PERMISSION_GRANTED

        val defaultSubId = PreferenceRepository(context).getDefaultSimSubscriptionId()

        return activeList?.map { sub ->
            val number = if (hasReadPhoneNumbers) {
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        subscriptionManager.getPhoneNumber(sub.subscriptionId)
                    } else {
                        @Suppress("DEPRECATION")
                        sub.number
                    }
                } catch (_: Exception) {
                    null
                }
            } else null

            val isDefault = if (defaultSubId != null) {
                sub.subscriptionId == defaultSubId
            } else {
                // If not set yet, check if it's the system default SMS subId
                sub.subscriptionId == SubscriptionManager.getDefaultSmsSubscriptionId()
            }

            SimCardInfo(
                slotIndex = sub.simSlotIndex,
                subscriptionId = sub.subscriptionId,
                displayName = sub.displayName?.toString() ?: "SIM ${sub.simSlotIndex + 1}",
                carrierName = sub.carrierName?.toString() ?: "Carrier ${sub.simSlotIndex + 1}",
                phoneNumber = number,
                isDefault = isDefault
            )
        } ?: emptyList()
    }
}
