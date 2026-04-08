package com.kakao.taxi.liveupdate

internal enum class NetworkSpeedUnit(val id: String) {
    AUTO("auto"),
    BYTES("b"),
    KILOBYTES("kb"),
    MEGABYTES("mb"),
    GIGABYTES("gb");

    companion object {
        fun from(raw: String?): NetworkSpeedUnit {
            return entries.firstOrNull { it.id == raw } ?: AUTO
        }
    }
}
