package com.pushctl.sdk

import android.annotation.SuppressLint
import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

@SuppressLint("ApplySharedPref")
internal class PushctlStore(context: Context) {
    private val preferences = context.getSharedPreferences("com.pushctl.sdk", Context.MODE_PRIVATE)

    val installationId: String
        get() {
            preferences.getString(INSTALLATION_ID, null)?.let { return it }
            return UUID.randomUUID().toString().also {
                preferences.edit().putString(INSTALLATION_ID, it).apply()
            }
        }

    var externalUserId: String?
        get() = preferences.getString(EXTERNAL_USER_ID, null)
        set(value) {
            preferences.edit().apply {
                if (value == null) remove(EXTERNAL_USER_ID) else putString(EXTERNAL_USER_ID, value)
            }.apply()
        }

    var pushToken: String?
        get() = preferences.getString(PUSH_TOKEN, null)
        set(value) = preferences.edit().putString(PUSH_TOKEN, value).apply()

    var providerIdentifierType: String
        get() = preferences.getString(PROVIDER_IDENTIFIER_TYPE, "fcm_fid") ?: "fcm_fid"
        set(value) = preferences.edit().putString(PROVIDER_IDENTIFIER_TYPE, value).apply()

    var permission: PushctlPermission
        get() = runCatching {
            PushctlPermission.valueOf(preferences.getString(PERMISSION, null) ?: PushctlPermission.UNKNOWN.name)
        }.getOrDefault(PushctlPermission.UNKNOWN)
        set(value) = preferences.edit().putString(PERMISSION, value.name).apply()

    @Synchronized
    fun enqueueEvent(event: JSONObject) {
        val events = queuedEvents()
        events.put(event)
        preferences.edit().putString(EVENTS, events.toString()).commit()
    }

    @Synchronized
    fun queuedEvents(): JSONArray = runCatching {
        JSONArray(preferences.getString(EVENTS, "[]"))
    }.getOrDefault(JSONArray())

    @Synchronized
    fun removeEvent(eventId: String) {
        val retained = JSONArray()
        val events = queuedEvents()
        for (index in 0 until events.length()) {
            val event = events.optJSONObject(index) ?: continue
            if (event.optString("event_id") != eventId) retained.put(event)
        }
        preferences.edit().putString(EVENTS, retained.toString()).commit()
    }

    fun saveConfiguration(configuration: PushctlConfiguration) {
        preferences.edit()
            .putString(APPLICATION_KEY, configuration.applicationKey)
            .putString(API_URL, configuration.apiUrl.trimEnd('/'))
            .putString(CHANNEL_ID, configuration.notificationChannelId)
            .putString(CHANNEL_NAME, configuration.notificationChannelName)
            .apply()
    }

    fun loadConfiguration(): PushctlConfiguration? {
        val key = preferences.getString(APPLICATION_KEY, null) ?: return null
        return PushctlConfiguration(
            applicationKey = key,
            apiUrl = preferences.getString(API_URL, null) ?: return null,
            notificationChannelId = preferences.getString(CHANNEL_ID, "pushctl_default") ?: "pushctl_default",
            notificationChannelName = preferences.getString(CHANNEL_NAME, "Notifications") ?: "Notifications",
        )
    }

    private companion object {
        const val INSTALLATION_ID = "installation_id"
        const val EXTERNAL_USER_ID = "external_user_id"
        const val PUSH_TOKEN = "push_token"
        const val PROVIDER_IDENTIFIER_TYPE = "provider_identifier_type"
        const val PERMISSION = "permission"
        const val EVENTS = "events"
        const val APPLICATION_KEY = "application_key"
        const val API_URL = "api_url"
        const val CHANNEL_ID = "channel_id"
        const val CHANNEL_NAME = "channel_name"
    }
}
