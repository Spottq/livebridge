package com.nowbar.api.fallback

internal data class FallbackResolution(
    val shouldPost: Boolean,
    val useStandardNotificationOnly: Boolean
)

internal object FallbackStrategyResolver {

    fun resolve(
        strategy: FallbackStrategy,
        nativeSurfaceSupported: Boolean
    ): FallbackResolution = when (strategy) {
        FallbackStrategy.NONE -> FallbackResolution(
            shouldPost = nativeSurfaceSupported,
            useStandardNotificationOnly = false
        )

        FallbackStrategy.STANDARD_NOTIFICATION -> FallbackResolution(
            shouldPost = true,
            useStandardNotificationOnly = true
        )

        FallbackStrategy.AUTO -> FallbackResolution(
            shouldPost = true,
            useStandardNotificationOnly = false
        )
    }
}
