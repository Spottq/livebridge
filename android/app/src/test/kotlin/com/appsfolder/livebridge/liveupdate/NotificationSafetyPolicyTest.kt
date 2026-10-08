package com.kakao.taxi.liveupdate

import org.junit.Assert.*
import org.junit.Test

class NotificationSafetyPolicyTest {
    @Test fun removalRepostLoopIsStoppedAndOtherSourcesAreUnaffected() {
        val policy = OriginalRemovalPolicy()
        assertTrue(policy.mayRemove("bluetooth", 0, false))
        assertTrue(policy.mayRemove("bluetooth", 1000, false))
        assertFalse(policy.mayRemove("bluetooth", 2000, false))
        assertFalse(policy.mayRemove("bluetooth", 61000, false))
        assertFalse(policy.mayRemove("bluetooth", 100000, false))
        assertTrue(policy.mayRemove("message", 100000, false))
        assertTrue(policy.mayRemove("bluetooth", 170001, false))
    }

    @Test fun persistentNotificationsNeverConsumeRemovalBudget() {
        val policy = OriginalRemovalPolicy()
        assertFalse(policy.mayRemove("service", 0, true))
        assertFalse(policy.mayRemove("service", 1000, true))
        assertTrue(policy.mayRemove("service", 2000, false))
    }

    @Test fun newContentRevivesDismissedNavigationButPollingDoesNot() {
        val policy = NotificationRevisionPolicy()
        policy.observe("map", "Turn left")
        policy.dismissed("map", setOf("map", "navigation-group"))
        assertTrue(policy.observe("map", "Turn left").isEmpty())
        assertEquals(setOf("map", "navigation-group"), policy.observe("map", "Turn right"))
        assertTrue(policy.observe("map", "Turn right").isEmpty())
    }

    @Test fun textExclusionsWinOverAllowedPhrasesAndMatchingIsLiteral() {
        val policy = TextFilterPolicy(allow = listOf("order"), deny = listOf("discount"))
        assertTrue(policy.allows("ORDER is ready"))
        assertFalse(policy.allows("Order discount"))
        assertFalse(policy.allows("Weather"))
        assertFalse(TextFilterPolicy(allow = listOf(".*")).allows("Anything"))
        assertTrue(TextFilterPolicy(allow = listOf(".*")).allows("literal .*"))
    }

    @Test fun allWordsAndEmptyAllowListBehaveAsExpected() {
        val policy = TextFilterPolicy(allow = listOf("订单", "ready"), all = true)
        assertTrue(policy.allows("订单 READY"))
        assertFalse(policy.allows("订单"))
        assertTrue(TextFilterPolicy(deny = listOf("ads")).allows("hello"))
        assertFalse(TextFilterPolicy(deny = listOf("ads")).allows("ADS"))
    }
    @Test fun customTextTemplatesSubstituteOnlyKnownTokensOnce() {
        assertEquals("Shop: {text} / ready", renderNotificationTemplate("{app}: {title} / {text}", "{text}", "ready", "Shop"))
        assertEquals("My delivery", renderNotificationTemplate("My delivery", "Title", "Body", "App"))
    }

    @Test fun emptyTemplatesKeepTheExistingPresentation() {
        assertNull(renderNotificationTemplate(null, "Title", "Body", "App"))
        assertNull(renderNotificationTemplate("  ", "Title", "Body", "App"))
    }

}
