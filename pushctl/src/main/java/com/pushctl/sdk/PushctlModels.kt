package com.pushctl.sdk

enum class PushctlPermission(val apiValue: String) {
    UNKNOWN("unknown"),
    NOT_DETERMINED("not_determined"),
    AUTHORIZED("authorized"),
    DENIED("denied"),
}

enum class PushctlEventType(val apiValue: String) {
    RECEIVED("received"),
    DISPLAYED("displayed"),
    OPENED("opened"),
    DISMISSED("dismissed"),
}

enum class PushctlRegistrationStatus(val apiValue: String) {
    UNREGISTERED("unregistered"),
    REGISTERING("registering"),
    REGISTERED("registered"),
    FAILED("failed"),
}

data class PushctlNotification(
    val notificationId: String,
    val deliveryId: String,
    val title: String?,
    val body: String?,
    val imageUrl: String?,
    val actionUrl: String?,
    val data: Map<String, String>,
)

data class PushctlSubscriptionState(
    val installationId: String,
    val externalUserId: String?,
    val permission: PushctlPermission,
    val pushToken: String?,
    val registrationStatus: PushctlRegistrationStatus,
    val registrationError: String?,
) {
    val isRegistered: Boolean
        get() = registrationStatus == PushctlRegistrationStatus.REGISTERED
}

fun interface PushctlNotificationListener {
    fun onNotification(notification: PushctlNotification)
}

fun interface PushctlOperationCallback {
    fun onComplete(error: Throwable?)
}

interface PushctlRegistrationListener {
    fun onRegistered(state: PushctlSubscriptionState)
    fun onRegistrationFailed(error: Throwable)
}

class PushctlRegistrationException(message: String, cause: Throwable? = null) : IllegalStateException(message, cause)
