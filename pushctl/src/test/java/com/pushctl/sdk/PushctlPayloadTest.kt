package com.pushctl.sdk

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PushctlPayloadTest {
    @Test
    fun `parses backend push envelope and preserves custom data`() {
        val notification = PushctlPayload.parse(mapOf(
            "_push" to "{\"notification_id\":\"notification-1\",\"delivery_id\":\"01K2DELIVERY\"}",
            "title" to "Hello",
            "body" to "World",
            "action_url" to "myapp://inbox",
            "account_id" to "42",
        ))

        requireNotNull(notification)
        assertEquals("notification-1", notification.notificationId)
        assertEquals("01K2DELIVERY", notification.deliveryId)
        assertEquals("Hello", notification.title)
        assertEquals("42", notification.data["account_id"])
        assertNull(notification.data["_push"])
    }

    @Test
    fun `ignores pushes without delivery metadata`() {
        assertNull(PushctlPayload.parse(mapOf("title" to "Not a Pushctl message")))
    }
}
