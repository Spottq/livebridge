package com.nowbar.api.util

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.Icon
import android.os.Build
import android.util.Log
import androidx.core.graphics.drawable.IconCompat

/**
 * Utility for loading application icons from any installed package for use in Now Bar.
 *
 * Supports standard Android icon loading and Samsung-specific icon tray format
 * (semGetApplicationIconForIconTray) for optimal Now Bar display.
 *
 * Usage:
 * ```
 * // Get icon as IconCompat for NowBarCard
 * val icon = AppIconHelper.getAppIconCompat(context, "com.mobile.legends")
 *
 * // Get icon as Icon for OngoingExtrasBuilder / ChipConfig
 * val icon = AppIconHelper.getAppIcon(context, "com.mobile.legends")
 *
 * // Samsung-optimized icon for icon tray
 * val icon = AppIconHelper.getSamsungTrayIcon(context, "com.mobile.legends")
 * ```
 */
object AppIconHelper {

    private const val TAG = "AppIconHelper"
    private const val DEFAULT_ICON_SIZE = 48

    /**
     * Loads the application icon for [packageName] as [IconCompat].
     * Suitable for NowBarCard.icon, toChipIcon(), toNowBarIcon(), etc.
     *
     * @return IconCompat of the app icon, or null if package not found.
     */
    fun getAppIconCompat(context: Context, packageName: String): IconCompat? {
        val bitmap = getAppIconBitmap(context, packageName) ?: return null
        return IconCompat.createWithBitmap(bitmap)
    }

    /**
     * Loads the application icon for [packageName] as [Icon].
     * Suitable for direct use with OngoingExtrasBuilder extras (chipIcon, nowbarIcon).
     *
     * @return Icon of the app, or null if package not found.
     */
    fun getAppIcon(context: Context, packageName: String): Icon? {
        val bitmap = getAppIconBitmap(context, packageName) ?: return null
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Icon.createWithAdaptiveBitmap(bitmap)
        } else {
            Icon.createWithBitmap(bitmap)
        }
    }

    /**
     * Samsung-specific: loads the icon tray version of the app icon via
     * semGetApplicationIconForIconTray (hidden Samsung API).
     *
     * Falls back to standard icon if the Samsung API is not available.
     *
     * @param size Icon size in dp (Samsung default is 48).
     * @return Icon for the app, or null if package not found.
     */
    fun getSamsungTrayIcon(context: Context, packageName: String, size: Int = DEFAULT_ICON_SIZE): Icon? {
        try {
            val pm = context.packageManager
            val method = pm.javaClass.getMethod(
                "semGetApplicationIconForIconTray",
                String::class.java,
                Int::class.javaPrimitiveType
            )
            val drawable = method.invoke(pm, packageName, size) as? Drawable
            if (drawable != null) {
                val bitmap = drawableToBitmap(drawable, size)
                return Icon.createWithBitmap(bitmap)
            }
        } catch (e: Exception) {
            Log.d(TAG, "semGetApplicationIconForIconTray not available, falling back to standard icon")
        }
        return getAppIcon(context, packageName)
    }

    /**
     * Samsung-specific tray icon as IconCompat.
     */
    fun getSamsungTrayIconCompat(context: Context, packageName: String, size: Int = DEFAULT_ICON_SIZE): IconCompat? {
        try {
            val pm = context.packageManager
            val method = pm.javaClass.getMethod(
                "semGetApplicationIconForIconTray",
                String::class.java,
                Int::class.javaPrimitiveType
            )
            val drawable = method.invoke(pm, packageName, size) as? Drawable
            if (drawable != null) {
                val bitmap = drawableToBitmap(drawable, size)
                return IconCompat.createWithBitmap(bitmap)
            }
        } catch (e: Exception) {
            Log.d(TAG, "semGetApplicationIconForIconTray not available, falling back")
        }
        return getAppIconCompat(context, packageName)
    }

    /**
     * Gets the app icon as a Bitmap.
     */
    fun getAppIconBitmap(context: Context, packageName: String, size: Int = DEFAULT_ICON_SIZE): Bitmap? {
        return try {
            val drawable = context.packageManager.getApplicationIcon(packageName)
            drawableToBitmap(drawable, size)
        } catch (e: PackageManager.NameNotFoundException) {
            Log.d(TAG, "Package not found: $packageName")
            null
        }
    }

    private fun drawableToBitmap(drawable: Drawable, size: Int = DEFAULT_ICON_SIZE): Bitmap {
        if (drawable is BitmapDrawable && drawable.bitmap != null) {
            return Bitmap.createScaledBitmap(drawable.bitmap, size, size, true)
        }

        val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else size
        val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else size
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return Bitmap.createScaledBitmap(bitmap, size, size, true)
    }
}
