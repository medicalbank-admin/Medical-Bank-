package com.example.notifications

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MedBankMessagingService : FirebaseMessagingService() {

    private val TAG = "MedBankMsgService"

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM_TOKEN", "Refreshed FCM token: $token")

        val currentUid = FirebaseAuth.getInstance().currentUser?.uid
        if (currentUid != null) {
            FirebaseFirestore.getInstance().collection("users").document(currentUid)
                .update("fcmToken", token)
                .addOnSuccessListener { Log.d("FCM_TOKEN", "Token saved to Firestore: $token") }
                .addOnFailureListener { e -> Log.e("FCM_TOKEN", "Failed to save token", e) }

            try {
                val dbId = getString(com.example.R.string.firestore_database_id)
                if (dbId.isNotBlank()) {
                    FirebaseFirestore.getInstance(dbId).collection("users").document(currentUid)
                        .update("fcmToken", token)
                        .addOnSuccessListener { Log.d("FCM_TOKEN", "Token saved to Firestore: $token") }
                        .addOnFailureListener { e -> Log.e("FCM_TOKEN", "Failed to save token", e) }
                }
            } catch (_: Exception) {}
        } else {
            Log.d("FCM_TOKEN", "User not logged in yet when onNewToken fired. Token will be saved on login.")
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
