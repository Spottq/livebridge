package com.kakao.taxi.liveupdate

internal object PromotedAccessPolicy {
    fun status(apiAvailable: Boolean, granted: Boolean?, settingsAvailable: Boolean): String = when {
        !apiAvailable -> "unavailable"
        granted == true -> "granted"
        granted == null -> "unknown"
        settingsAvailable -> "denied"
        else -> "unavailable"
    }
}
