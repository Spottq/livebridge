package com.nowbar.api.fallback

/**
 * Behaviour for SDK-managed notification posting and rendering.
 *
 * Foreground-service helpers still need a visible notification to satisfy Android.
 */
enum class FallbackStrategy {
    /**
     * Post nothing on devices without a native enhanced surface.
     *
     * On supported Samsung / Android 16 devices the SDK still uses the native-enhanced path.
     * Foreground services still have to keep their required notification.
     */
    NONE,

    /**
     * Always use a plain ongoing notification.
     *
     * Native Samsung extras and Android 16 promoted ongoing features are not requested.
     */
    STANDARD_NOTIFICATION,

    /**
     * Prefer native Samsung / Android 16 surfaces when available,
     * otherwise keep a plain ongoing notification.
     */
    AUTO
}
