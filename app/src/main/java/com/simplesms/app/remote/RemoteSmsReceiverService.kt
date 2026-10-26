package com.simplesms.app.remote

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class RemoteSmsReceiverService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM Token received: $token")
        val deviceId = WebGatewayManager.getDeviceId(this)
        try {
            FirebaseFirestore.getInstance()
                .collection("devices")
                .document(deviceId)
                .update("fcmToken", token)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to upload new FCM token", e)
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "FCM Remote Push Received: ${remoteMessage.data}")

        if (!WebGatewayManager.isGatewayEnabled(this)) {
            Log.w(TAG, "Gateway disabled, ignoring push message")
            return
        }

        val data = remoteMessage.data
        val recipient = data["recipient"] ?: return
        val message = data["message"] ?: return
        val messageId = data["messageId"] ?: "fcm_${System.currentTimeMillis()}"
        val deviceId = data["deviceId"] ?: WebGatewayManager.getDeviceId(this)

        WebGatewayManager.processIncomingFirestoreSms(
            context = this,
            deviceId = deviceId,
            messageId = messageId,
            recipient = recipient,
            message = message
        )
    }

    companion object {
        private const val TAG = "RemoteSmsReceiver"
    }
}
