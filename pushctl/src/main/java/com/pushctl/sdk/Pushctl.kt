package com.pushctl.sdk

import android.Manifest
import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import org.json.JSONObject
import java.util.concurrent.CopyOnWriteArraySet

object Pushctl {
    private const val PERMISSION_REQUEST_CODE = 9401
    private var applicationContext: Context? = null
    private var configuration: PushctlConfiguration? = null
    private var store: PushctlStore? = null
    private var client: PushctlApiClient? = null
    private var lifecycleCallbacksRegistered = false
    @Volatile private var startedActivityCount = 0
    private val clickListeners = CopyOnWriteArraySet<PushctlNotificationListener>()
    private val foregroundListeners = CopyOnWriteArraySet<PushctlNotificationListener>()
    private var pendingClick: PushctlNotification? = null

    @JvmStatic
    @JvmOverloads
    fun initialize(
        context: Context,
        applicationKey: String,
        apiUrl: String = "https://pushctl.com/api/v1",
    ) = initialize(context, PushctlConfiguration(applicationKey, apiUrl))

    @JvmStatic
    fun initialize(context: Context, configuration: PushctlConfiguration) {
        val appContext = context.applicationContext
        val sdkStore = PushctlStore(appContext)
        sdkStore.saveConfiguration(configuration)
        sdkStore.permission = currentPermission(appContext)
        applicationContext = appContext
        this.configuration = configuration
        store = sdkStore
        client = PushctlApiClient(appContext, configuration, sdkStore)
        registerLifecycleCallbacks(appContext)
        initializeFirebase(appContext)
        FirebaseMessaging.getInstance().register()
        client?.flush()
    }

    internal fun initializeFirebase(context: Context): FirebaseApp {
        FirebaseApp.getApps(context).firstOrNull { it.name == FirebaseApp.DEFAULT_APP_NAME }?.let { return it }

        return checkNotNull(FirebaseApp.initializeApp(context)) {
            "Firebase could not be initialized. Add android/app/google-services.json and apply com.google.gms.google-services to the app module."
        }
    }

    @JvmStatic
    fun requestPermission(activity: Activity) {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.POST_NOTIFICATIONS), PERMISSION_REQUEST_CODE)
        } else {
            syncPermission()
        }
    }

    @JvmStatic
    fun syncPermission() {
        val context = applicationContext ?: return
        val permission = currentPermission(context)
        val sdkStore = store ?: return
        if (sdkStore.permission == permission) return
        sdkStore.permission = permission
        sdkStore.pushToken?.let { client?.register(it) }
    }

    @JvmStatic
    fun login(externalUserId: String) {
        require(externalUserId.isNotBlank()) { "externalUserId must not be blank" }
        val sdkStore = requireStore()
        sdkStore.externalUserId = externalUserId
        client?.update(JSONObject().put("user_id", externalUserId))
    }

    @JvmStatic
    fun logout() {
        requireStore().externalUserId = null
        client?.update(JSONObject().put("user_id", JSONObject.NULL))
    }

    @JvmStatic
    fun subscriptionState(): PushctlSubscriptionState {
        val sdkStore = requireStore()
        return PushctlSubscriptionState(sdkStore.installationId, sdkStore.externalUserId, sdkStore.permission, sdkStore.pushToken)
    }

    @JvmStatic
    fun addNotificationClickListener(listener: PushctlNotificationListener): Boolean {
        val added = clickListeners.add(listener)
        pendingClick?.let {
            pendingClick = null
            listener.onNotification(it)
        }
        return added
    }

    @JvmStatic
    fun removeNotificationClickListener(listener: PushctlNotificationListener) = clickListeners.remove(listener)

    @JvmStatic
    fun addForegroundNotificationListener(listener: PushctlNotificationListener) = foregroundListeners.add(listener)

    @JvmStatic
    fun removeForegroundNotificationListener(listener: PushctlNotificationListener) = foregroundListeners.remove(listener)

    internal fun bootstrap(context: Context): Boolean {
        if (applicationContext != null) return true
        val sdkStore = PushctlStore(context.applicationContext)
        val saved = sdkStore.loadConfiguration() ?: return false
        initialize(context, saved)
        return true
    }

    internal fun setPushIdentifier(identifier: String, type: String = "fcm_fid") {
        val sdkStore = store ?: return
        sdkStore.providerIdentifierType = type
        sdkStore.pushToken = identifier
        client?.register(identifier)
    }

    internal fun report(type: PushctlEventType, notification: PushctlNotification) {
        client?.report(type, notification)
    }

    internal fun notifyClick(notification: PushctlNotification) {
        if (clickListeners.isEmpty()) {
            pendingClick = notification
            return
        }
        clickListeners.forEach { it.onNotification(notification) }
    }

    internal fun notifyForeground(notification: PushctlNotification) {
        foregroundListeners.forEach { it.onNotification(notification) }
    }

    internal val isForeground: Boolean
        get() = startedActivityCount > 0

    private fun registerLifecycleCallbacks(context: Context) {
        if (lifecycleCallbacksRegistered) return
        val application = context as? Application ?: return
        lifecycleCallbacksRegistered = true
        application.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityResumed(activity: Activity) {
                syncPermission()
                handleNotificationOpen(activity.intent)
            }
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
            override fun onActivityStarted(activity: Activity) {
                startedActivityCount += 1
            }
            override fun onActivityPaused(activity: Activity) = Unit
            override fun onActivityStopped(activity: Activity) {
                startedActivityCount = (startedActivityCount - 1).coerceAtLeast(0)
            }
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
            override fun onActivityDestroyed(activity: Activity) = Unit
        })
    }

    @JvmStatic
    fun handleNotificationOpen(intent: Intent?) {
        if (intent?.getStringExtra(PushctlNotificationActionReceiver.EXTRA_ACTION) != PushctlNotificationActionReceiver.ACTION_OPEN) return
        val notification = PushctlNotificationActionReceiver.parse(intent.getStringExtra(PushctlNotificationActionReceiver.EXTRA_NOTIFICATION)) ?: return
        intent.removeExtra(PushctlNotificationActionReceiver.EXTRA_ACTION)
        intent.removeExtra(PushctlNotificationActionReceiver.EXTRA_NOTIFICATION)
        report(PushctlEventType.OPENED, notification)
        notifyClick(notification)
    }

    private fun requireStore(): PushctlStore = store ?: error("Call Pushctl.initialize() first")

    private fun currentPermission(context: Context): PushctlPermission {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return PushctlPermission.DENIED
        return if (Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            PushctlPermission.AUTHORIZED
        } else {
            PushctlPermission.NOT_DETERMINED
        }
    }
}
