package com.example.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R

object NotificationHelper {

    const val CHANNEL_DEFAULT = "medbank_default"
    const val CHANNEL_APPROVALS = "medbank_approvals"
    const val CHANNEL_NEW_EXAMS = "medbank_new_exams"

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Default channel
            val defaultChannel = NotificationChannel(
                CHANNEL_DEFAULT,
                "General Announcements",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "General announcements and updates from MedBank"
            }

            // Approvals channel (High importance)
            val approvalsChannel = NotificationChannel(
                CHANNEL_APPROVALS,
                "Access Approvals",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications when your access requests are approved"
                enableLights(true)
                enableVibration(true)
            }

            // New exams channel
            val examsChannel = NotificationChannel(
                CHANNEL_NEW_EXAMS,
                "New Exams",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Announcements when new exams are published"
            }

            notificationManager.createNotificationChannels(listOf(defaultChannel, approvalsChannel, examsChannel))
        }
    }

    fun showNotification(
        context: Context,
        title: String,
        body: String,
        channelId: String = CHANNEL_DEFAULT,
        route: String? = null
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            if (!route.isNullOrBlank()) {
                putExtra("route", route)
            }
        }

        val pendingIntentFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val pendingIntent = PendingIntent.getActivity(context, System.currentTimeMillis().toInt(), intent, pendingIntentFlag)

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(
                if (channelId == CHANNEL_APPROVALS) NotificationCompat.PRIORITY_HIGH
                else NotificationCompat.PRIORITY_DEFAULT
            )

        try {
            with(NotificationManagerCompat.from(context)) {
                notify(System.currentTimeMillis().toInt(), builder.build())
            }
        } catch (e: SecurityException) {
            // Notification permission not granted on Android 13+
        }
    }
}
