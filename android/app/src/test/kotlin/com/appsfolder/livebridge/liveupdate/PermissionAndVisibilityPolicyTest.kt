package com.kakao.taxi.liveupdate

import org.junit.Assert.*
import org.junit.Test

class PermissionAndVisibilityPolicyTest {
    @Test fun promotionDenialIsOnlyActionableWhenSettingsExist() {
        assertEquals("denied", PromotedAccessPolicy.status(true, false, true))
        assertEquals("unavailable", PromotedAccessPolicy.status(true, false, false))
        assertEquals("unavailable", PromotedAccessPolicy.status(false, null, true))
    }
    @Test fun grantedAccessDoesNotNeedASettingsPage() {
        assertEquals("granted", PromotedAccessPolicy.status(true, true, false))
    }
    @Test fun failedApiCheckDoesNotPretendPermissionWasGrantedOrDenied() {
        assertEquals("unknown", PromotedAccessPolicy.status(true, null, true))
    }
    @Test fun speedVisibilityKeepsExistingBehaviorByDefault() {
        for (off in listOf(false, true)) for (locked in listOf(false, true))
            assertTrue(NetworkSpeedVisibilityPolicy.allowPromotion(false, off, locked))
    }
    @Test fun speedCapsuleReturnsOnlyAfterUnlocking() {
        assertFalse(NetworkSpeedVisibilityPolicy.allowPromotion(true, true, false))
        assertFalse(NetworkSpeedVisibilityPolicy.allowPromotion(true, true, true))
        assertFalse(NetworkSpeedVisibilityPolicy.allowPromotion(true, false, true))
        assertTrue(NetworkSpeedVisibilityPolicy.allowPromotion(true, false, false))
    }
}
