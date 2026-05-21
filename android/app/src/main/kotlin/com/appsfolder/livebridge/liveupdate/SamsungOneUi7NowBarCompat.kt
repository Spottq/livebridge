package com.kakao.taxi.liveupdate

import android.app.Notification

internal object SamsungOneUi7NowBarCompat {
    private const val SEM_ONGOING_ACTIVITY_FLAG = 0x8000

    fun markEligible(notification: Notification): Notification {
        if (!DeviceProps.isSamsungDevice()) {
            return notification
        }

        runCatching {
            val semFlagsField = Notification::class.java.getDeclaredField("semFlags")
            semFlagsField.isAccessible = true
            val currentFlags = semFlagsField.getInt(notification)
            semFlagsField.setInt(
                notification,
                currentFlags or SEM_ONGOING_ACTIVITY_FLAG
            )
        }

        return notification
    }
}
