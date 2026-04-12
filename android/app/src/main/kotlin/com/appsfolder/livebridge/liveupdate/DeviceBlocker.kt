package com.kakao.taxi.liveupdate

object DeviceBlocker {
    fun isBlockedDevice(): Boolean {
        return !DeviceProps.isSamsungDevice()
    }
}
