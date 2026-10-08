package com.kakao.taxi.liveupdate

internal object NetworkSpeedVisibilityPolicy {
    fun allowPromotion(hideWhenLocked: Boolean, screenOff: Boolean, keyguardLocked: Boolean): Boolean =
        !hideWhenLocked || (!screenOff && !keyguardLocked)
}
