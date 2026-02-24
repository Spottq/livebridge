package ai.perplexity.app.android.liveupdate

import android.os.Build

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
