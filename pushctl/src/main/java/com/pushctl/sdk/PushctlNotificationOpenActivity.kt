package com.pushctl.sdk

import android.app.Activity
import android.content.Intent
import android.os.Bundle

class PushctlNotificationOpenActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Pushctl.bootstrap(this)) {
            PushctlNotificationActionReceiver.parse(
                intent.getStringExtra(PushctlNotificationActionReceiver.EXTRA_NOTIFICATION),
            )?.let { notification ->
                Pushctl.report(PushctlEventType.OPENED, notification)
                Pushctl.notifyClick(notification)
            }
        }

        packageManager.getLaunchIntentForPackage(packageName)?.let { launchIntent ->
            launchIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(launchIntent)
        }
        finish()
    }
}
