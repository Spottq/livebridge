package com.kakao.taxi.liveupdate

import org.junit.Assert.*
import org.junit.Test

class SourceNotificationLifecycleTest {
    @Test fun missingRemovalCallbackIsRecoveredWithoutRemovingOtherApps() {
        val state = SourceNotificationLifecycle()
        state.posted("read-message")
        state.posted("navigation")
        assertEquals(setOf("read-message"), state.reconcile(setOf("navigation"), setOf("read-message", "navigation")))
        assertTrue(state.reconcile(setOf("navigation"), setOf("navigation")).isEmpty())
    }

    @Test fun intentionalOriginalRemovalKeepsItsMirrorUntilDismissed() {
        val state = SourceNotificationLifecycle()
        state.posted("otp")
        state.retainMirrorAfterRemoval("otp")
        assertFalse(state.removed("otp"))
        assertTrue(state.reconcile(emptySet(), setOf("otp")).isEmpty())
        assertEquals(setOf("otp"), state.reconcile(emptySet(), emptySet()))
    }

    @Test fun repostedKeyIsARealSourceAgain() {
        val state = SourceNotificationLifecycle()
        state.posted("chat")
        state.retainMirrorAfterRemoval("chat")
        state.posted("chat")
        assertTrue(state.removed("chat"))
        assertTrue(state.reconcile(emptySet(), emptySet()).isEmpty())
    }

    @Test fun failedOriginalCancellationDoesNotRetainAGhost() {
        val state = SourceNotificationLifecycle()
        state.posted("message")
        state.retainMirrorAfterRemoval("message")
        state.cancelRetention("message")
        assertEquals(setOf("message"), state.reconcile(emptySet(), setOf("message")))
    }

    @Test fun activeUnreadMessageIsNotDismissedJustBecauseAnAppWasOpened() {
        val state = SourceNotificationLifecycle()
        state.posted("chat")
        assertTrue(state.reconcile(setOf("chat"), setOf("chat")).isEmpty())
    }
}
