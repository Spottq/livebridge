package com.waymo.carapp.liveupdate

internal enum class NetworkSpeedDisplayMode(val id: String) {
    TOTAL("total"),
    UPLOAD("upload"),
    DOWNLOAD("download");

    companion object {
        fun from(raw: String?): NetworkSpeedDisplayMode {
            return entries.firstOrNull { it.id == raw } ?: TOTAL
        }
    }
}
