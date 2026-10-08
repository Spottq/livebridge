package com.kakao.taxi.liveupdate

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.os.SystemClock
import android.util.LruCache
import androidx.core.graphics.drawable.IconCompat

/** Bounded cache of application assets; source notification icons and album art remain live. */
internal object AppMetadataCache {
    private val entries = LruCache<String, Entry>(32)
    private val bitmaps = object : LruCache<String, Bitmap>(8 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.allocationByteCount
    }
    private const val TTL_MS = 5 * 60_000L

    @Synchronized
    fun get(context: Context, packageName: String): Entry {
        val config = context.resources.configuration
        val key = "$packageName|${config.locales.toLanguageTags()}|${config.densityDpi}|${config.uiMode}"
        val now = SystemClock.elapsedRealtime()
        entries.get(key)?.takeIf { now - it.createdAt < TTL_MS }?.let { return it }
        bitmaps.remove(key)
        return Entry(context.applicationContext, packageName, key, now).also { entries.put(key, it) }
    }

    @Synchronized
    private fun largeIcon(context: Context, packageName: String, key: String): Bitmap? {
        bitmaps.get(key)?.let { return it }
        return runCatching {
            val drawable = context.packageManager.getApplicationIcon(packageName)
            val bitmap = if (drawable is BitmapDrawable && drawable.bitmap != null) {
                drawable.bitmap
            } else {
                val width = drawable.intrinsicWidth.coerceIn(1, 512)
                val height = drawable.intrinsicHeight.coerceIn(1, 512)
                Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also {
                    val canvas = Canvas(it)
                    drawable.setBounds(0, 0, width, height)
                    drawable.draw(canvas)
                }
            }
            bitmaps.put(key, bitmap)
            bitmap
        }.getOrNull()
    }

    class Entry(
        private val context: Context,
        private val packageName: String,
        private val key: String,
        val createdAt: Long
    ) {
        val label: String by lazy {
            runCatching {
                val info = context.packageManager.getApplicationInfo(packageName, 0)
                context.packageManager.getApplicationLabel(info).toString().ifBlank { packageName }
            }.getOrDefault(packageName)
        }
        val smallIcon: IconCompat? by lazy {
            runCatching {
                val info = context.packageManager.getApplicationInfo(packageName, 0)
                if (info.icon == 0) null else IconCompat.createWithResource(
                    context.createPackageContext(packageName, 0).resources, packageName, info.icon
                )
            }.getOrNull()
        }
        val largeIcon: Bitmap? get() = largeIcon(context, packageName, key)
    }
}
