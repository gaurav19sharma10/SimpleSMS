package com.simplesms.app.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.simplesms.app.MainActivity
import com.simplesms.app.R
import com.simplesms.app.SimpleSmsApp
import com.simplesms.app.data.model.ContactItem
import com.simplesms.app.data.model.SendStatus
import com.simplesms.app.data.model.SmsResult
import com.simplesms.app.data.repository.PreferenceRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SmsDispatchService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var dispatchJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val subId = intent.getIntExtra(EXTRA_SUB_ID, -1)
                val template = intent.getStringExtra(EXTRA_TEMPLATE) ?: ""
                startForegroundWithNotification()
                startBatchDispatch(subId, template)
            }
            ACTION_CANCEL -> {
                cancelDispatch()
            }
        }
        return START_NOT_STICKY
    }

    private fun startForegroundWithNotification() {
        val notification = buildProgressNotification(
            title = "Preparing SMS Dispatch",
            content = "Initializing...",
            current = 0,
            total = _queuedContacts.size
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            } else {
                0
            }
            startForeground(NOTIFICATION_ID, notification, type)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun startBatchDispatch(subscriptionId: Int, template: String) {
        dispatchJob?.cancel()
        dispatchJob = serviceScope.launch {
            _isDispatching.value = true
            val total = _queuedContacts.size
            val prefs = PreferenceRepository(this@SmsDispatchService)
            val delayMs = prefs.getThrottleDelayMs()

            for (index in _queuedContacts.indices) {
                val contact = _queuedContacts[index]

                // Mark current contact as SENDING
                _contactStatusUpdates.value = ContactStatusEvent(
                    contactId = contact.id,
                    status = SendStatus.SENDING
                )
                _currentProgress.value = Pair(index + 1, total)

                updateNotification(
                    title = "Sending SMS (${index + 1}/$total)",
                    content = "To: ${contact.name} (${contact.phoneNumber})",
                    current = index + 1,
                    total = total
                )

                // Personalize template with {name}
                val personalizedMessage = template.replace("{name}", contact.name, ignoreCase = true)

                // Dispatch via cellular baseband
                val result = SmsDispatcher.sendSms(
                    context = this@SmsDispatchService,
                    destinationAddress = contact.phoneNumber,
                    messageText = personalizedMessage,
                    subscriptionId = subscriptionId
                )

                when (result) {
                    is SmsResult.Success -> {
                        _contactStatusUpdates.value = ContactStatusEvent(
                            contactId = contact.id,
                            status = SendStatus.SENT
                        )
                    }
                    is SmsResult.Error -> {
                        _contactStatusUpdates.value = ContactStatusEvent(
                            contactId = contact.id,
                            status = SendStatus.FAILED,
                            error = result.reason
                        )
                    }
                    is SmsResult.Progress -> {
                        // Intermediate progress
                    }
                }

                // Throttle pacing between sequential SMS to prevent carrier/OS spam blocking
                if (index < total - 1) {
                    delay(delayMs)
                }
            }

            _isDispatching.value = false
            updateNotification(
                title = "SMS Dispatch Complete",
                content = "Finished sending to $total contact(s)",
                current = total,
                total = total,
                ongoing = false
            )

            // Auto stop after 3 seconds
            delay(3000)
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun cancelDispatch() {
        dispatchJob?.cancel()
        _isDispatching.value = false
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun buildProgressNotification(
        title: String,
        content: String,
        current: Int,
        total: Int,
        ongoing: Boolean = true
    ): Notification {
        val openAppIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val cancelIntent = PendingIntent.getService(
            this, 1,
            Intent(this, SmsDispatchService::class.java).apply { action = ACTION_CANCEL },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, SimpleSmsApp.CHANNEL_ID_DISPATCH)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(content)
            .setContentIntent(openAppIntent)
            .setOngoing(ongoing)
            .setProgress(total, current, total == 0)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Cancel", cancelIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification(
        title: String,
        content: String,
        current: Int,
        total: Int,
        ongoing: Boolean = true
    ) {
        val notification = buildProgressNotification(title, content, current, total, ongoing)
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? android.app.NotificationManager
        manager?.notify(NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    data class ContactStatusEvent(
        val contactId: String,
        val status: SendStatus,
        val error: String? = null
    )

    companion object {
        const val NOTIFICATION_ID = 1001
        const val ACTION_START = "com.simplesms.app.ACTION_START"
        const val ACTION_CANCEL = "com.simplesms.app.ACTION_CANCEL"
        const val EXTRA_SUB_ID = "extra_subscription_id"
        const val EXTRA_TEMPLATE = "extra_message_template"

        private val _queuedContacts = mutableListOf<ContactItem>()

        private val _isDispatching = MutableStateFlow(false)
        val isDispatching: StateFlow<Boolean> = _isDispatching.asStateFlow()

        private val _contactStatusUpdates = MutableStateFlow<ContactStatusEvent?>(null)
        val contactStatusUpdates: StateFlow<ContactStatusEvent?> = _contactStatusUpdates.asStateFlow()

        private val _currentProgress = MutableStateFlow(Pair(0, 0))
        val currentProgress: StateFlow<Pair<Int, Int>> = _currentProgress.asStateFlow()

        fun setQueue(contacts: List<ContactItem>) {
            _queuedContacts.clear()
            _queuedContacts.addAll(contacts)
        }

        fun start(context: Context, subscriptionId: Int, template: String, contacts: List<ContactItem>) {
            setQueue(contacts)
            val intent = Intent(context, SmsDispatchService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_SUB_ID, subscriptionId)
                putExtra(EXTRA_TEMPLATE, template)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, SmsDispatchService::class.java).apply {
                action = ACTION_CANCEL
            }
            context.startService(intent)
        }
    }
}
