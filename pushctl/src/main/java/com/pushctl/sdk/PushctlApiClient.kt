package com.pushctl.sdk

import android.content.Context
import android.os.Build
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import java.util.concurrent.Executors

internal class PushctlApiClient(
    context: Context,
    private val configuration: PushctlConfiguration,
    private val store: PushctlStore,
) {
    private val applicationContext = context.applicationContext
    private val executor = Executors.newSingleThreadExecutor()

    fun register(token: String, completion: ((Throwable?) -> Unit)? = null) {
        executor.execute {
            val error = runCatching {
                request("PUT", "installations/${store.installationId}", registrationPayload(token))
            }.exceptionOrNull()
            completion?.invoke(error)
            if (error == null) flushEvents()
        }
    }

    fun update(fields: JSONObject, completion: ((Throwable?) -> Unit)? = null) {
        executor.execute {
            val error = runCatching {
                request("PATCH", "installations/${store.installationId}", fields)
            }.exceptionOrNull()
            completion?.invoke(error)
        }
    }

    fun report(type: PushctlEventType, notification: PushctlNotification) {
        val eventId = UUID.nameUUIDFromBytes(
            "${store.installationId}:${notification.deliveryId}:${type.apiValue}".toByteArray(Charsets.UTF_8),
        ).toString()
        val event = JSONObject()
            .put("event_id", eventId)
            .put("delivery_id", notification.deliveryId)
            .put("installation_id", store.installationId)
            .put("type", type.apiValue)
            .put("occurred_at", eventDateFormatter().format(Date()))
            .put("metadata", JSONObject().put("sdk", "android").put("sdk_version", SDK_VERSION))
        store.enqueueEvent(event)
        executor.execute { flushEvents() }
    }

    fun flush() {
        executor.execute { flushEvents() }
    }

    private fun flushEvents() {
        val events = store.queuedEvents()
        for (index in 0 until events.length()) {
            val event = events.optJSONObject(index) ?: continue
            val sent = runCatching { request("POST", "events", event) }.isSuccess
            if (!sent) return
            store.removeEvent(event.getString("event_id"))
        }
    }

    private fun registrationPayload(token: String): JSONObject {
        val packageInfo = applicationContext.packageManager.getPackageInfo(applicationContext.packageName, 0)
        return JSONObject()
            .put("user_id", store.externalUserId ?: JSONObject.NULL)
            .put("platform", "android")
            .put("provider", JSONObject().put("type", store.providerIdentifierType).put("identifier", token))
            .put("permission", store.permission.apiValue)
            .put("device", JSONObject()
                .put("app_version", packageInfo.versionName ?: JSONObject.NULL)
                .put("os_version", Build.VERSION.RELEASE)
                .put("model", "${Build.MANUFACTURER} ${Build.MODEL}".trim())
                .put("locale", Locale.getDefault().toLanguageTag())
                .put("timezone", TimeZone.getDefault().id))
    }

    private fun request(method: String, path: String, body: JSONObject) {
        val connection = URL("${configuration.apiUrl.trimEnd('/')}/$path").openConnection() as HttpURLConnection
        try {
            connection.requestMethod = method
            connection.connectTimeout = 10_000
            connection.readTimeout = 15_000
            connection.doOutput = true
            connection.setRequestProperty("Authorization", "Bearer ${configuration.applicationKey}")
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("User-Agent", "Pushctl-Android/$SDK_VERSION")
            connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val status = connection.responseCode
            if (status !in 200..299) {
                val response = connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                throw PushctlException(status, response)
            }
        } finally {
            connection.disconnect()
        }
    }

    private companion object {
        const val SDK_VERSION = "0.2.0"

        fun eventDateFormatter() = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'+00:00'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
    }
}

class PushctlException(val statusCode: Int, response: String) : RuntimeException(
    "Pushctl API request failed with HTTP $statusCode${response.takeIf(String::isNotBlank)?.let { ": $it" }.orEmpty()}",
)
