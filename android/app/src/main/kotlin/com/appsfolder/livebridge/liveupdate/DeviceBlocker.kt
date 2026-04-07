package com.kakao.taxi.liveupdate

import android.os.Build
import java.util.Locale

object DeviceBlocker {
    fun isBlockedDevice(): Boolean {
        val manufacturer = Build.MANUFACTURER?.lowercase(Locale.ROOT).orEmpty()
        val brand = Build.BRAND?.lowercase(Locale.ROOT).orEmpty()
        val mn = DeviceProps.marketName().lowercase(Locale.ROOT)
        val product = Build.PRODUCT?.lowercase(Locale.ROOT).orEmpty()

        return (manufacturer.contains("google") || brand.contains("google")) &&
                (mn.contains("pixel") || product.contains("pixel"))
    }
}
