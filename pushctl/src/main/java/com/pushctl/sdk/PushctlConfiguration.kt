package com.pushctl.sdk

data class PushctlConfiguration(
    val applicationKey: String,
    val apiUrl: String = "https://pushctl.com/api/v1",
    val notificationChannelId: String = "pushctl_default",
    val notificationChannelName: String = "Notifications",
    val notificationIcon: Int? = null,
) {
    init {
        require(applicationKey.isNotBlank()) { "applicationKey must not be blank" }
        require(apiUrl.startsWith("https://") || apiUrl.startsWith("http://localhost") || apiUrl.contains(".test")) {
            "apiUrl must use HTTPS outside local development"
        }
    }
}
