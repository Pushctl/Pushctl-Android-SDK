package com.pushctl.sdk

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class PushctlFirebaseMessagingService : FirebaseMessagingService() {
    override fun onRegistered(installationId: String) {
        if (Pushctl.bootstrap(this)) Pushctl.setPushIdentifier(installationId)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        if (!Pushctl.bootstrap(this)) return
        val notification = PushctlPayload.parse(message.data) ?: return
        Pushctl.report(PushctlEventType.RECEIVED, notification)
        if (Pushctl.isForeground) Pushctl.notifyForeground(notification)
        display(notification)
    }

    private fun display(notification: PushctlNotification) {
        val store = PushctlStore(this)
        val configuration = store.loadConfiguration() ?: return
        createChannel(configuration)
        val intent = Intent(this, PushctlNotificationOpenActivity::class.java).apply {
            putExtra(PushctlNotificationActionReceiver.EXTRA_ACTION, PushctlNotificationActionReceiver.ACTION_OPEN)
            putExtra(PushctlNotificationActionReceiver.EXTRA_NOTIFICATION, notificationToJson(notification))
        }
        val contentIntent = PendingIntent.getActivity(
            this,
            notification.deliveryId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val dismissIntent = Intent(this, PushctlNotificationActionReceiver::class.java).apply {
            putExtra(PushctlNotificationActionReceiver.EXTRA_ACTION, PushctlNotificationActionReceiver.ACTION_DISMISS)
            putExtra(PushctlNotificationActionReceiver.EXTRA_NOTIFICATION, notificationToJson(notification))
        }
        val deleteIntent = PendingIntent.getBroadcast(
            this,
            notification.deliveryId.hashCode(),
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val icon = configuration.notificationIcon ?: applicationInfo.icon
        val builder = NotificationCompat.Builder(this, configuration.notificationChannelId)
            .setSmallIcon(icon)
            .setContentTitle(notification.title)
            .setContentText(notification.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(notification.body))
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .setDeleteIntent(deleteIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        if (NotificationManagerCompat.from(this).areNotificationsEnabled()) {
            NotificationManagerCompat.from(this).notify(notification.deliveryId.hashCode(), builder.build())
            Pushctl.report(PushctlEventType.DISPLAYED, notification)
        }
    }

    private fun createChannel(configuration: PushctlConfiguration) {
        if (Build.VERSION.SDK_INT < 26) return
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(NotificationChannel(
            configuration.notificationChannelId,
            configuration.notificationChannelName,
            NotificationManager.IMPORTANCE_HIGH,
        ))
    }

    private fun notificationToJson(notification: PushctlNotification): String = org.json.JSONObject()
        .put("notification_id", notification.notificationId)
        .put("delivery_id", notification.deliveryId)
        .put("title", notification.title)
        .put("body", notification.body)
        .put("image_url", notification.imageUrl)
        .put("action_url", notification.actionUrl)
        .put("data", org.json.JSONObject(notification.data))
        .toString()
}
