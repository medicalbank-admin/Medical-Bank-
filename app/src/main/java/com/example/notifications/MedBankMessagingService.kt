package com.example.notifications

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MedBankMessagingService : FirebaseMessagingService() {

    private val TAG = "MedBankMsgService"

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Refreshed FCM token: $token")
        
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            updateTokenInFirestore(currentUser.uid, token)
        }
    }

    private fun updateTokenInFirestore(uid: String, token: String, retryCount: Int = 1) {
        val firestore = FirebaseFirestore.getInstance()
        val data = mapOf(
            "fcmToken" to token,
            "fcmTokenUpdatedAt" to FieldValue.serverTimestamp()
        )
        firestore.collection("users").document(uid)
            .update(data)
            .addOnSuccessListener {
                Log.d(TAG, "FCM token updated successfully for $uid")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to update FCM token (retry: $retryCount): ${e.message}")
                if (retryCount > 0) {
                    // Retry once
                    updateTokenInFirestore(uid, token, retryCount - 1)
                }
            }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Log.d(TAG, "Message received from: ${message.from}")

        val title = message.notification?.title ?: message.data["title"] ?: "MedBank Notification"
        val body = message.notification?.body ?: message.data["body"] ?: "You have a new update in MedBank."
        val route = message.data["route"] ?: message.notification?.tag
        val channelId = message.data["channelId"] ?: NotificationHelper.CHANNEL_DEFAULT

        NotificationHelper.showNotification(
            context = applicationContext,
            title = title,
            body = body,
            channelId = channelId,
            route = route
        )
    }
}
