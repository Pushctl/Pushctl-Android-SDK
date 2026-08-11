# Pushctl Android SDK

Native Android push notifications with automatic installation registration and delivery analytics.

Create a **Client** token in Pushctl for the mobile app. Never embed a Full or Server token.

## Install

Add the package and configure Firebase in the app as usual:

```kotlin
dependencies {
    implementation("com.pushctl:pushctl-android:0.1.0")
}
```

Initialize once from your `Application` class:

```kotlin
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        Pushctl.initialize(this, "push_live_your_mobile_key")
    }
}
```

Ask for permission from an Activity when the timing is right:

```kotlin
Pushctl.requestPermission(this)
```

Associate the device with your signed-in user. Logging in again replaces the association; logout keeps the installation subscribed anonymously.

```kotlin
Pushctl.login(user.id)
Pushctl.logout()
```

Handle notification opens:

```kotlin
Pushctl.addNotificationClickListener { notification ->
    notification.actionUrl?.let(router::open)
}
```

The SDK automatically tracks `received`, `displayed`, `opened`, and `dismissed` events. Events are assigned idempotency IDs, persisted before sending, and retried whenever the SDK starts, registers, or receives another event.

For self-hosting or local development, pass your API root as the third argument:

```kotlin
Pushctl.initialize(this, key, "https://push.example.com/api/v1")
```

The host app must apply the Google Services plugin and include its Firebase `google-services.json`. No custom `FirebaseMessagingService` is required.
