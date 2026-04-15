package com.kakao.taxi.liveupdate

import android.app.Notification
import androidx.core.graphics.drawable.IconCompat

internal data class FlashlightSourceSnapshot(
    val iconCompat: IconCompat?,
    val accentColor: Int
)

internal object FlashlightSourceState {
    private val lock = Any()
    private var iconCompat: IconCompat? = null
    private var accentColor: Int = DEFAULT_ACCENT_COLOR

    fun updateFrom(notification: Notification) {
        synchronized(lock) {
            iconCompat = runCatching {
                notification.smallIcon?.let(IconCompat::createFromIcon)
            }.getOrNull()
            accentColor = notification.color.takeIf { it != 0 } ?: DEFAULT_ACCENT_COLOR
        }
    }

    fun clear() {
        synchronized(lock) {
            iconCompat = null
            accentColor = DEFAULT_ACCENT_COLOR
        }
    }

    fun snapshot(): FlashlightSourceSnapshot {
        return synchronized(lock) {
            FlashlightSourceSnapshot(
                iconCompat = iconCompat,
                accentColor = accentColor
            )
        }
    }

    private const val DEFAULT_ACCENT_COLOR = 0xFF387AFF.toInt()
}
