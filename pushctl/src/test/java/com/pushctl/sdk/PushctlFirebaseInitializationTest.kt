package com.pushctl.sdk

import android.content.pm.PackageManager
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import org.junit.After
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class PushctlFirebaseInitializationTest {
    @Before
    @After
    fun deleteFirebaseApps() {
        FirebaseApp.getApps(RuntimeEnvironment.getApplication()).forEach(FirebaseApp::delete)
    }

    @Test
    fun `uses an existing default Firebase app`() {
        val context = RuntimeEnvironment.getApplication()
        val firebaseApp = FirebaseApp.initializeApp(
            context,
            FirebaseOptions.Builder()
                .setApplicationId("1:123456789:android:test")
                .setApiKey("test-api-key")
                .build(),
        )

        assertSame(firebaseApp, Pushctl.initializeFirebase(context))
    }

    @Test
    fun `enables Firebase installation id messaging`() {
        val context = RuntimeEnvironment.getApplication()
        val applicationInfo = context.packageManager.getApplicationInfo(
            context.packageName,
            PackageManager.GET_META_DATA,
        )

        assertTrue(applicationInfo.metaData.getBoolean("firebase_messaging_installation_id_enabled"))
    }

    @Test
    fun `reports actionable error when Firebase configuration is unavailable`() {
        val context = RuntimeEnvironment.getApplication()

        val exception = assertThrows(IllegalStateException::class.java) {
            Pushctl.initializeFirebase(context)
        }

        assertTrue(exception.message.orEmpty().contains("google-services.json"))
    }
}
