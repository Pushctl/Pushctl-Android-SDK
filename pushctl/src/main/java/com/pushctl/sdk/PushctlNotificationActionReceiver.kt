package com.pushctl.sdk

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.json.JSONObject

class PushctlNotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (!Pushctl.bootstrap(context)) return
        val notification = parse(intent.getStringExtra(EXTRA_NOTIFICATION)) ?: return
        when (intent.getStringExtra(EXTRA_ACTION)) {
            ACTION_OPEN -> {
                Pushctl.report(PushctlEventType.OPENED, notification)
                Pushctl.notifyClick(notification)
            }
            ACTION_DISMISS -> Pushctl.report(PushctlEventType.DISMISSED, notification)
        }
    }

    companion object {
        internal fun parse(value: String?): PushctlNotification? = runCatching {
        val json = JSONObject(value ?: return null)
        val dataJson = json.optJSONObject("data") ?: JSONObject()
        val data = buildMap {
            dataJson.keys().forEach { key -> put(key, dataJson.optString(key)) }
        }
        PushctlNotification(
            notificationId = json.getString("notification_id"),
            deliveryId = json.getString("delivery_id"),
            title = json.optString("title").takeIf(String::isNotBlank),
            body = json.optString("body").takeIf(String::isNotBlank),
            imageUrl = json.optString("image_url").takeIf(String::isNotBlank),
            actionUrl = json.optString("action_url").takeIf(String::isNotBlank),
            data = data,
        )
    }.getOrNull()

        const val EXTRA_ACTION = "com.pushctl.sdk.ACTION"
        const val EXTRA_NOTIFICATION = "com.pushctl.sdk.NOTIFICATION"
        const val ACTION_OPEN = "open"
        const val ACTION_DISMISS = "dismiss"
    }
}
