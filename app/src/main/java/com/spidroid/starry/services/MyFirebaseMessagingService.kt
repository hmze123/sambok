package com.spidroid.starry.services

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.TaskStackBuilder
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.spidroid.starry.R
import com.spidroid.starry.activities.ChatActivity
import com.spidroid.starry.activities.MainActivity
import com.spidroid.starry.activities.PostDetailActivity
import com.spidroid.starry.activities.ProfileActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "From: ${remoteMessage.from}")

        val title = remoteMessage.notification?.title
        val body = remoteMessage.notification?.body
        val data = remoteMessage.data

        sendNotification(title, body, data)
    }

    override fun onNewToken(token: String) {
        Log.d(TAG, "Refreshed token: $token")
        sendRegistrationToServer(token)
    }

    private fun sendRegistrationToServer(token: String?) {
        val userId = Firebase.auth.currentUser?.uid
        if (userId != null && token != null) {
            Firebase.firestore.collection("users").document(userId)
                .update("fcmToken", token)
                .addOnSuccessListener { Log.d(TAG, "FCM token updated successfully for user $userId") }
                .addOnFailureListener { e -> Log.w(TAG, "Error updating FCM token", e) }
        }
    }

    private fun sendNotification(title: String?, messageBody: String?, data: Map<String, String>) {
        val clickAction = data["click_action"]

        // إنشاء الـ Intent الأساسي الذي يفتح الشاشة الرئيسية
        val mainIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        // إنشاء الـ Intent المخصص بناءً على نوع الإشعار
        val targetIntent: Intent = when (clickAction) {
            "POST" -> Intent(this, PostDetailActivity::class.java).apply {
                putExtra("postId", data["post_id"])
            }
            "PROFILE" -> Intent(this, ProfileActivity::class.java).apply {
                putExtra("userId", data["user_id"])
            }
            "CHAT" -> Intent(this, ChatActivity::class.java).apply {
                putExtra("chatId", data["chat_id"])
            }
            else -> mainIntent // Intent افتراضي
        }

        // إنشاء TaskStackBuilder لضمان سلوك زر الرجوع الصحيح
        val pendingIntent: PendingIntent? = TaskStackBuilder.create(this).run {
            addNextIntentWithParentStack(targetIntent)
            getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }

        val channelId = getString(R.string.default_notification_channel_id)
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notifications_filled)
            .setContentTitle(title ?: "Starry")
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelName = getString(R.string.default_notification_channel_name)
            val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH)
            val notificationManager: NotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        with(NotificationManagerCompat.from(this)) {
            if (ActivityCompat.checkSelfPermission(this@MyFirebaseMessagingService, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "Notification permission not granted.")
                return
            }
            notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
        }
    }

    companion object {
        private const val TAG = "MyFirebaseMsgService"
    }
}