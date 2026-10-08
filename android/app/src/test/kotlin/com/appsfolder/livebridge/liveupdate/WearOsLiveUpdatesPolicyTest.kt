package com.kakao.taxi.liveupdate

import org.junit.Assert.*
import org.junit.Test

class WearOsLiveUpdatesPolicyTest {
    @Test fun requiresAndroid17OrLater() {
        assertFalse(WearOsLiveUpdatesPolicy.isAvailable(36))
        assertTrue(WearOsLiveUpdatesPolicy.isAvailable(37))
        assertTrue(WearOsLiveUpdatesPolicy.isAvailable(38))
    }

    @Test fun restoredEnabledPreferenceCannotBridgeOnOlderAndroid() {
        assertTrue(WearOsLiveUpdatesPolicy.isLocalOnly(35, true))
        assertTrue(WearOsLiveUpdatesPolicy.isLocalOnly(36, true))
    }

    @Test fun bridgingRequiresOptInEvenOnSupportedAndroid() {
        assertTrue(WearOsLiveUpdatesPolicy.isLocalOnly(37, false))
        assertFalse(WearOsLiveUpdatesPolicy.isLocalOnly(37, true))
        assertFalse(WearOsLiveUpdatesPolicy.isLocalOnly(38, true))
    }
}
