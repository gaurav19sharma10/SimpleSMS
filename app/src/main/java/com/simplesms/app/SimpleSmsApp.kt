package com.simplesms.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class SimpleSmsApp : Application() {

    override fun onCreate() {
        super.onCreate()
        try {
            if (com.google.firebase.FirebaseApp.getApps(this).isEmpty()) {
                com.google.firebase.FirebaseApp.initializeApp(this)
            }
        } catch (_: Exception) {}
        createNotificationChannels()
        if (com.simplesms.app.remote.WebGatewayManager.isGatewayEnabled(this)) {
            com.simplesms.app.remote.WebGatewayManager.startGateway(this)
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val dispatchChannel = NotificationChannel(
                CHANNEL_ID_DISPATCH,
                getString(R.string.channel_sms_dispatch_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.channel_sms_dispatch_desc)
                enableVibration(false)
                setShowBadge(false)
            }

            val gatewayChannel = NotificationChannel(
                CHANNEL_ID_GATEWAY,
                getString(R.string.channel_sms_gateway_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.channel_sms_gateway_desc)
                enableVibration(false)
                setShowBadge(false)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(dispatchChannel)
            notificationManager?.createNotificationChannel(gatewayChannel)
        }
    }

    companion object {
        const val CHANNEL_ID_DISPATCH = "sms_dispatch_channel"
        const val CHANNEL_ID_GATEWAY = "sms_gateway_channel"
    }
}
