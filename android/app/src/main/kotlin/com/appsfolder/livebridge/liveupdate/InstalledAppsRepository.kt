package com.kakao.taxi.liveupdate

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.Log
import java.io.ByteArrayOutputStream
import java.util.Locale

internal object InstalledAppsRepository {
    private const val TAG = "InstalledAppsRepository"
    private const val INSTALLED_APPS_CACHE_TTL_MS = 24 * 60 * 60 * 1000L
    private const val MAX_ICON_CACHE_SIZE = 512
    private const val ICON_SIZE_PX = 96

    private val cacheLock = Any()
    private var installedAppsCache: List<Map<String, Any>>? = null
    private var installedAppsCacheAtMs: Long = 0L
    private val appIconBytesCache: MutableMap<String, ByteArray> = mutableMapOf()

    fun loadInstalledApps(
        context: Context,
        selfPackageName: String,
        forceRefresh: Boolean = false
    ): List<Map<String, Any>> {
        val now = System.currentTimeMillis()
        synchronized(cacheLock) {
            val cached = installedAppsCache
            if (!forceRefresh &&
                cached != null &&
                now - installedAppsCacheAtMs <= INSTALLED_APPS_CACHE_TTL_MS
            ) {
                return cached
            }
        }

        val pm = context.packageManager
        val launcherIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolved = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(
                launcherIntent,
                PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_ALL.toLong())
            )
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(launcherIntent, PackageManager.MATCH_ALL)
        }

        val entriesByPackage = linkedMapOf<String, MutableMap<String, Any>>()
        val iconsByPackage = linkedMapOf<String, ByteArray>()

        resolved.forEach { resolveInfo ->
            val activityInfo = resolveInfo.activityInfo ?: return@forEach
            val appPackage = activityInfo.packageName.orEmpty()
            if (appPackage.isEmpty() || appPackage == selfPackageName) {
                return@forEach
            }
            val resolvedLabel = resolveInfo.loadLabel(pm)?.toString()?.trim().orEmpty()
            val label = if (resolvedLabel.isNotEmpty()) resolvedLabel else appPackage
            val iconBytes = resolveCachedIconBytes(appPackage) ?: drawableToPngBytes(resolveInfo.loadIcon(pm))
            val isSystemApp = isSystemApp(activityInfo.applicationInfo)
            val entry = mutableMapOf<String, Any>(
                "packageName" to appPackage,
                "label" to label,
                "isSystem" to isSystemApp
            )
            if (iconBytes != null) {
                entry["icon"] = iconBytes
                iconsByPackage[appPackage] = iconBytes
            }
            entriesByPackage[appPackage] = entry
        }

        getInstalledPackagesCompat(pm).forEach { packageInfo ->
            val appInfo = packageInfo.applicationInfo ?: return@forEach
            val appPackage = packageInfo.packageName.orEmpty()
            if (appPackage.isEmpty() || appPackage == selfPackageName) {
                return@forEach
            }
            if (!isSystemApp(appInfo) || entriesByPackage.containsKey(appPackage)) {
                return@forEach
            }
            val label = appInfo.loadLabel(pm)?.toString()?.trim().orEmpty().ifEmpty { appPackage }
            val iconBytes = resolveCachedIconBytes(appPackage) ?: drawableToPngBytes(appInfo.loadIcon(pm))
            val entry = mutableMapOf<String, Any>(
                "packageName" to appPackage,
                "label" to label,
                "isSystem" to true
            )
            if (iconBytes != null) {
                entry["icon"] = iconBytes
                iconsByPackage[appPackage] = iconBytes
            }
            entriesByPackage[appPackage] = entry
        }

        val entries = entriesByPackage.values
            .sortedBy { (it["label"] as? String)?.lowercase(Locale.getDefault()) ?: "" }
            .toList()
        val refreshedAt = System.currentTimeMillis()

        synchronized(cacheLock) {
            installedAppsCache = entries
            installedAppsCacheAtMs = refreshedAt
            appIconBytesCache.clear()
            appIconBytesCache.putAll(iconsByPackage)
        }
        return entries
    }

    private fun loadPackageIconPngBytes(context: Context, packageName: String): ByteArray? {
        val drawable = try {
            context.packageManager.getApplicationInfo(packageName, 0).loadIcon(context.packageManager)
        } catch (_: Throwable) {
            null
        } ?: return null

        val bytes = drawableToPngBytes(drawable) ?: return null
        cacheIconBytes(packageName, bytes)
        return bytes
    }

    private fun isSystemApp(applicationInfo: ApplicationInfo?): Boolean {
        if (applicationInfo == null) {
            return false
        }
        val flags = applicationInfo.flags
        return (flags and ApplicationInfo.FLAG_SYSTEM) != 0 ||
            (flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
    }

    private fun getInstalledPackagesCompat(pm: PackageManager): List<PackageInfo> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getInstalledPackages(PackageManager.PackageInfoFlags.of(0L))
        } else {
            @Suppress("DEPRECATION")
            pm.getInstalledPackages(0)
        }
    }

    private fun resolveCachedIconBytes(packageName: String): ByteArray? {
        synchronized(cacheLock) {
            return appIconBytesCache[packageName]
        }
    }

    private fun cacheIconBytes(packageName: String, bytes: ByteArray) {
        synchronized(cacheLock) {
            if (appIconBytesCache.size >= MAX_ICON_CACHE_SIZE &&
                !appIconBytesCache.containsKey(packageName)
            ) {
                appIconBytesCache.clear()
            }
            appIconBytesCache[packageName] = bytes
        }
    }

    private fun drawableToPngBytes(drawable: Drawable?): ByteArray? {
        drawable ?: return null
        return try {
            val bitmap = Bitmap.createBitmap(ICON_SIZE_PX, ICON_SIZE_PX, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, ICON_SIZE_PX, ICON_SIZE_PX)
            drawable.draw(canvas)

            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            outputStream.toByteArray()
        } catch (error: Throwable) {
            Log.w(TAG, "Failed to extract application icon bytes for $drawable", error)
            null
        }
    }
}
