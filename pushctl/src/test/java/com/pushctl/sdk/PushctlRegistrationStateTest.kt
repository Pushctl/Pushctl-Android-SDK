package com.pushctl.sdk

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PushctlRegistrationStateTest {
    @Test
    fun `only confirmed registration is ready`() {
        val registered = state(PushctlRegistrationStatus.REGISTERED)
        val registering = state(PushctlRegistrationStatus.REGISTERING)

        assertTrue(registered.isRegistered)
        assertFalse(registering.isRegistered)
    }

    private fun state(status: PushctlRegistrationStatus) = PushctlSubscriptionState(
        installationId = "installation-1",
        externalUserId = null,
        permission = PushctlPermission.AUTHORIZED,
        pushToken = "token",
        registrationStatus = status,
        registrationError = null,
    )
}
