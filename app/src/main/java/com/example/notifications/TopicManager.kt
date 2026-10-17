package com.example.notifications

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await

object TopicManager {
    private const val TAG = "TopicManager"

    fun subscribeUserTopics(uid: String, programType: String, mbbsYear: Int?, licensingPath: String?, notificationOptIn: Boolean) {
        val messaging = FirebaseMessaging.getInstance()
        val topicsToSubscribe = mutableListOf<String>()

        if (!notificationOptIn) {
            // If opted out, unsubscribe from all standard topics
            unsubscribeAll(uid, listOf("mbbs_students", "licensing_students"))
            return
        }

        if (programType == "mbbs") {
            topicsToSubscribe.add("mbbs_students")
            if (mbbsYear != null) {
                topicsToSubscribe.add("mbbs_year_$mbbsYear")
            }
        } else if (programType == "licensing") {
            topicsToSubscribe.add("licensing_students")
            if (!licensingPath.isNullOrBlank()) {
                topicsToSubscribe.add("${licensingPath.lowercase().trim()}_students")
            }
        }

        // Also general announcements topic
        topicsToSubscribe.add("medbank_announcements")

        for (topic in topicsToSubscribe) {
            messaging.subscribeToTopic(topic)
                .addOnSuccessListener {
                    Log.d(TAG, "Subscribed to topic: $topic")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to subscribe to topic: $topic", e)
                }
        }

        // Save subscribed topics to Firestore
        val firestore = FirebaseFirestore.getInstance()
        firestore.collection("users").document(uid)
            .update(
                mapOf(
                    "subscribedTopics" to topicsToSubscribe,
                    "notificationOptIn" to notificationOptIn
                )
            )
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to update subscribedTopics in Firestore", e)
            }
    }

    fun unsubscribeTopic(uid: String, topic: String, currentTopics: List<String>) {
        FirebaseMessaging.getInstance().unsubscribeFromTopic(topic)
            .addOnSuccessListener {
                Log.d(TAG, "Unsubscribed from topic: $topic")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to unsubscribe from topic: $topic", e)
            }

        val updated = currentTopics.filter { it != topic }
        val firestore = FirebaseFirestore.getInstance()
        firestore.collection("users").document(uid)
            .update("subscribedTopics", updated)
    }

    fun subscribeTopic(uid: String, topic: String, currentTopics: List<String>) {
        FirebaseMessaging.getInstance().subscribeToTopic(topic)
            .addOnSuccessListener {
                Log.d(TAG, "Subscribed to topic: $topic")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to subscribe to topic: $topic", e)
            }

        val updated = (currentTopics + topic).distinct()
        val firestore = FirebaseFirestore.getInstance()
        firestore.collection("users").document(uid)
            .update("subscribedTopics", updated)
    }

    private fun unsubscribeAll(uid: String, topics: List<String>) {
        val messaging = FirebaseMessaging.getInstance()
        for (topic in topics) {
            messaging.unsubscribeFromTopic(topic)
        }
        val firestore = FirebaseFirestore.getInstance()
        firestore.collection("users").document(uid)
            .update(
                mapOf(
                    "subscribedTopics" to emptyList<String>(),
                    "notificationOptIn" to false
                )
            )
    }
}
