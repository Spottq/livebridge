package com.nowbar.api.fallback

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FallbackStrategyResolverTest {

    @Test
    fun `none skips unsupported devices`() {
        val resolution = FallbackStrategyResolver.resolve(
            strategy = FallbackStrategy.NONE,
            nativeSurfaceSupported = false
        )

        assertFalse(resolution.shouldPost)
        assertFalse(resolution.useStandardNotificationOnly)
    }

    @Test
    fun `none keeps native path on supported devices`() {
        val resolution = FallbackStrategyResolver.resolve(
            strategy = FallbackStrategy.NONE,
            nativeSurfaceSupported = true
        )

        assertTrue(resolution.shouldPost)
        assertFalse(resolution.useStandardNotificationOnly)
    }

    @Test
    fun `standard notification always stays plain`() {
        val resolution = FallbackStrategyResolver.resolve(
            strategy = FallbackStrategy.STANDARD_NOTIFICATION,
            nativeSurfaceSupported = true
        )

        assertTrue(resolution.shouldPost)
        assertTrue(resolution.useStandardNotificationOnly)
    }

    @Test
    fun `auto keeps fallback on unsupported devices`() {
        val resolution = FallbackStrategyResolver.resolve(
            strategy = FallbackStrategy.AUTO,
            nativeSurfaceSupported = false
        )

        assertTrue(resolution.shouldPost)
        assertFalse(resolution.useStandardNotificationOnly)
    }
}
