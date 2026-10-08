package com.kakao.taxi.liveupdate

internal object WearOsLiveUpdatesPolicy {
    // Android 17 (API 37). Wear OS 7 adds native bridging of ongoing Live Updates.
    fun isAvailable(sdkInt: Int): Boolean = sdkInt >= 37

    fun isLocalOnly(sdkInt: Int, enabled: Boolean): Boolean =
        !isAvailable(sdkInt) || !enabled
}
