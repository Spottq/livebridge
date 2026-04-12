package com.kakao.taxi.liveupdate

import android.os.Build
import java.util.Locale

object DeviceProps {
    private val marketNameKeys = listOf(
        "ro.product.marketname",
        "ro.vendor.product.marketname",
        "ro.config.marketing_name",
        "ro.product.odm.marketname",
        "ro.vendor.product.display",
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
        val manufacturer = (Build.MANUFACTURER ?: "").lowercase(Locale.ROOT)
        val brand = (Build.BRAND ?: "").lowercase(Locale.ROOT)
        return manufacturer.contains("samsung") || brand.contains("samsung")
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
