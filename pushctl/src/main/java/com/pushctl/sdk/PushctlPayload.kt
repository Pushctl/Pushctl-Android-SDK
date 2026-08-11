package com.pushctl.sdk

import org.json.JSONObject

internal object PushctlPayload {
    fun parse(data: Map<String, String>): PushctlNotification? {
        val metadata = data["_push"]?.let {
            runCatching { JSONObject(it) }.getOrNull()
        } ?: return null

        val notificationId = metadata.optString("notification_id").takeIf(String::isNotBlank) ?: return null
        val deliveryId = metadata.optString("delivery_id").takeIf(String::isNotBlank) ?: return null

        return PushctlNotification(
            notificationId = notificationId,
            deliveryId = deliveryId,
            title = data["title"],
            body = data["body"],
            imageUrl = data["image_url"],
            actionUrl = data["action_url"],
            data = data.filterKeys { it !in SDK_KEYS },
        )
    }

    private val SDK_KEYS = setOf("_push", "title", "body", "image_url", "action_url")
}
