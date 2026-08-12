# Pushctl Android SDK

Native Android push notifications with automatic installation registration and delivery analytics.

Create a **Client** token in Pushctl for the mobile app. Never embed a Full or Server token.

## Install

The private package is hosted in GitHub Packages. Create a GitHub personal access token (classic) with `read:packages`, then add these values to your user-level `~/.gradle/gradle.properties`:

```properties
gpr.user=YOUR_GITHUB_USERNAME
gpr.key=YOUR_GITHUB_TOKEN
```

Add the private registry and package, then configure Firebase in the app as usual:

```kotlin
repositories {
    google()
    mavenCentral()
    maven {
        url = uri("https://maven.pkg.github.com/flowgistics/Pushctl-Android-SDK")
        credentials {
            username = providers.gradleProperty("gpr.user").orNull
            password = providers.gradleProperty("gpr.key").orNull
        }
    }
}
```

```kotlin
dependencies {
    implementation("com.pushctl:pushctl-android:0.1.1")
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

## Releasing

Push a semantic-version tag to publish the Maven package and create a GitHub release:

```shell
git tag v0.1.1
git push origin v0.1.1
```
