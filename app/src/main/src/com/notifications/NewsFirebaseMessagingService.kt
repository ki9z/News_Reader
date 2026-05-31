package com.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.ui.main.MainActivity

class NewsFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val title = message.notification?.title ?: message.data[KEY_TITLE] ?: getString(R.string.app_name)
        val body = message.notification?.body ?: message.data[KEY_BODY] ?: return
        val articleUrl = message.data[KEY_ARTICLE_URL]

        showBreakingNewsNotification(
            title = title,
            body = body,
            articleUrl = articleUrl
        )
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        DeviceTokenRegistrar.registerAsync(this, token)
    }

    private fun showBreakingNewsNotification(title: String, body: String, articleUrl: String?) {
        createChannelIfNeeded()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_ARTICLE_URL, articleUrl)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            articleUrl.orEmpty().hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_BREAKING_NEWS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(this).notify(articleUrl.orEmpty().hashCode(), notification)
    }

    private fun createChannelIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_BREAKING_NEWS,
            getString(R.string.profile_push_notifications),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = getString(R.string.profile_toggle_notifications)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    companion object {
        private const val CHANNEL_BREAKING_NEWS = "breaking_news"
        private const val KEY_TITLE = "title"
        private const val KEY_BODY = "body"
        private const val KEY_ARTICLE_URL = "articleUrl"
    }
}
