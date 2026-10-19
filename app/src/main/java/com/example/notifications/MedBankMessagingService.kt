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
        }
        // Ensure subscribed to global topic on new token
        com.google.firebase.messaging.FirebaseMessaging.getInstance().subscribeToTopic("all_students")
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "Message received from: ${remoteMessage.from}")

        // Use your existing NotificationHelper to show the notification
        remoteMessage.notification?.let {
            NotificationHelper.showNotification(
                context = this,
                title = it.title ?: "Medical Bank",
                body = it.body ?: "New update available",
                channelId = "medbank_default",
                route = remoteMessage.data["route"] ?: "home"
            )
            return
        }

        // Fallback for data-only messages
        val dataTitle = remoteMessage.data["title"] ?: "Medical Bank"
        val dataBody = remoteMessage.data["body"] ?: "New update available"
        val dataRoute = remoteMessage.data["route"] ?: "home"
        val channelId = remoteMessage.data["channelId"] ?: NotificationHelper.CHANNEL_DEFAULT
        NotificationHelper.showNotification(
            context = this,
            title = dataTitle,
            body = dataBody,
            channelId = channelId,
            route = dataRoute
        )
    }
}
