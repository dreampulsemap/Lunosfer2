package io.lunosfer.dreamap.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import io.lunosfer.dreamap.MainActivity
import io.lunosfer.dreamap.R
import io.lunosfer.dreamap.data.model.PushSubscriptionRequest
import io.lunosfer.dreamap.data.network.NetworkModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LunosferMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM token: $token")
        sendTokenToServer(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "From: ${remoteMessage.from}")

        val title = remoteMessage.notification?.title 
            ?: remoteMessage.data["title"] 
            ?: "Lunosfer"
        val body = remoteMessage.notification?.body 
            ?: remoteMessage.data["body"] 
            ?: remoteMessage.data["message"] 
            ?: ""

        showNotification(title, body, remoteMessage.data)
    }

    private fun showNotification(title: String, message: String, data: Map<String, String>) {
        val targetRoute = parseTargetRoute(data)

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            if (!targetRoute.isNullOrBlank()) {
                putExtra("target_route", targetRoute)
            }
        }

        val requestCode = System.currentTimeMillis().toInt()
        val pendingIntent = PendingIntent.getActivity(
            this, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = CHANNEL_ID
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Lunosfer Bildirimleri",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Lunosfer mesaj ve etkileşim bildirimleri"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(requestCode, notification)
    }

    companion object {
        private const val TAG = "LunosferFCM"
        const val CHANNEL_ID = "lunosfer_notifications"

        fun parseTargetRoute(data: Map<String, String>): String? {
            val rawUrl = data["url"] ?: data["target_route"]
            if (!rawUrl.isNullOrBlank()) {
                val cleanUrl = rawUrl.trim().removePrefix("/")
                if (cleanUrl.isNotBlank()) {
                    return cleanUrl
                }
            }

            val type = data["type"]?.lowercase()
            val id = data["id"] ?: data["target_id"] ?: data["entity_id"]
            if (!type.isNullOrBlank()) {
                return when (type) {
                    "dream", "dream_detail" -> if (!id.isNullOrBlank()) "dream/$id" else null
                    "thread", "message", "chat" -> if (!id.isNullOrBlank()) "thread/$id" else null
                    "goal", "vision", "goal_detail" -> if (!id.isNullOrBlank()) "goal/$id" else null
                    "user", "profile" -> if (!id.isNullOrBlank()) "public_profile/$id" else null
                    "notification", "notifications" -> "notifications"
                    else -> null
                }
            }

            return null
        }

        fun sendTokenToServer(token: String) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    NetworkModule.api.subscribePush(PushSubscriptionRequest(token = token))
                    Log.d(TAG, "FCM token successfully registered to server")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to send FCM token to server (silently ignored)", e)
                }
            }
        }

        fun registerCurrentFcmToken() {
            try {
                com.google.firebase.messaging.FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val token = task.result
                        if (!token.isNullOrEmpty()) {
                            sendTokenToServer(token)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Could not fetch FCM token", e)
            }
        }
    }
}
