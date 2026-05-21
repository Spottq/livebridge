package com.kakao.taxi.liveupdate

import android.os.Build
import java.util.Locale

object DeviceProps {
    private const val DEBUG_FORCE_NON_SAMSUNG_PROP = "debug.livebridge.force_non_samsung"
    private val marketNameKeys = listOf(
        "ro.product.marketname",
        "ro.config.marketing_name",
        "ro.product.odm.marketname",
        "ro.product.vendor.marketname"
    )

    fun marketName(): String {
        for (k in marketNameKeys) {
            val res = readSystemProperty(k)
            if (!res.isNullOrBlank()) {
                return res
            }
        }
        return Build.MODEL ?: ""
    }

    fun isSamsungDevice(): Boolean {
        val forced = readSystemProperty(DEBUG_FORCE_NON_SAMSUNG_PROP)
        if (forced == "1" || forced.equals("true", ignoreCase = true)) {
            return false
        }
        val manufacturer = (Build.MANUFACTURER ?: "").lowercase(Locale.ROOT)
        val brand = (Build.BRAND ?: "").lowercase(Locale.ROOT)
        return manufacturer.contains("samsung") || brand.contains("samsung")
    }

    fun isSamsungOneUi7Android15(): Boolean {
        return isSamsungDevice() && Build.VERSION.SDK_INT == 35
    }

    private fun readSystemProperty(key: String): String? {
        return try {
            val clazz = Class.forName("android.os.SystemProperties")
            val getter = clazz.getMethod("get", String::class.java)
            val res = getter.invoke(null, key) as? String
            res
                ?.trim()
                ?.takeIf { it.isNotEmpty() && !it.equals("unknown", ignoreCase = true) }
        } catch (_: Throwable) {
            null
        }
    }
}
