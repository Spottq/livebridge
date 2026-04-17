package com.kakao.taxi.liveupdate

import android.app.Notification
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.core.graphics.drawable.IconCompat

internal data class FlashlightSourceSnapshot(
    val iconCompat: IconCompat?,
    val largeIconBitmap: Bitmap?,
    val accentColor: Int
)

internal object FlashlightSourceState {
    private val lock = Any()
    private var iconCompat: IconCompat? = null
    private var largeIconBitmap: Bitmap? = null
    private var accentColor: Int = DEFAULT_ACCENT_COLOR

    fun updateFrom(context: Context, packageName: String, notification: Notification) {
        synchronized(lock) {
            val packageContext = runCatching {
                context.createPackageContext(packageName, 0)
            }.getOrNull()

            val resolvedBitmap = resolveSmallIconBitmap(packageContext ?: context, notification)
            val resolvedIcon = resolvedBitmap?.let {
                runCatching { IconCompat.createWithBitmap(it) }.getOrNull()
            } ?: resolveSmallIconCompat(packageContext ?: context, notification)

            iconCompat = resolvedIcon
            largeIconBitmap = resolvedBitmap
            accentColor = notification.color.takeIf { it != 0 } ?: DEFAULT_ACCENT_COLOR
        }
    }

    fun clear() {
        synchronized(lock) {
            iconCompat = null
            largeIconBitmap = null
            accentColor = DEFAULT_ACCENT_COLOR
        }
    }

    fun snapshot(): FlashlightSourceSnapshot {
        return synchronized(lock) {
            FlashlightSourceSnapshot(
                iconCompat = iconCompat,
                largeIconBitmap = largeIconBitmap,
                accentColor = accentColor
            )
        }
    }

    private fun resolveSmallIconCompat(
        context: Context,
        notification: Notification
    ): IconCompat? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val frameworkSmallIcon = notification.smallIcon
            if (frameworkSmallIcon != null) {
                return runCatching {
                    IconCompat.createFromIcon(context, frameworkSmallIcon)
                }.getOrNull()
            }
        }

        @Suppress("DEPRECATION")
        val legacyIconRes = notification.icon
        if (legacyIconRes == 0) {
            return null
        }

        return runCatching {
            IconCompat.createWithResource(context, legacyIconRes)
        }.getOrNull()
    }

    private fun resolveSmallIconBitmap(
        context: Context,
        notification: Notification
    ): Bitmap? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val frameworkSmallIcon = notification.smallIcon
            if (frameworkSmallIcon != null) {
                runCatching {
                    frameworkSmallIcon.loadDrawable(context)?.let(::drawableToBitmap)
                }.getOrNull()?.let { return it }
            }
        }

        @Suppress("DEPRECATION")
        val legacyIconRes = notification.icon
        if (legacyIconRes == 0) {
            return null
        }

        return runCatching {
            context.getDrawable(legacyIconRes)?.let(::drawableToBitmap)
        }.getOrNull()
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        if (drawable is BitmapDrawable && drawable.bitmap != null) {
            return drawable.bitmap
        }
        val width = drawable.intrinsicWidth.coerceAtLeast(1).coerceAtMost(512)
        val height = drawable.intrinsicHeight.coerceAtLeast(1).coerceAtMost(512)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    private const val DEFAULT_ACCENT_COLOR = 0xFF387AFF.toInt()
}
