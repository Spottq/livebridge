package com.waymo.carapp.liveupdate

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

        fun parseSelection(raw: String?): Set<NetworkSpeedUnit> {
            return raw.orEmpty()
                .split(',')
                .mapNotNull { token ->
                    entries.firstOrNull { it.id == token.trim() }
                }
                .toSet()
        }

        fun normalizeSelection(raw: String?): String {
            if (raw == null) {
                return AUTO.id
            }

            val selected = parseSelection(raw)
            if (selected.isEmpty()) {
                return ""
            }
            if (selected.contains(AUTO)) {
                return AUTO.id
            }

            return entries
                .filter { it != AUTO && selected.contains(it) }
                .joinToString(",") { it.id }
        }
    }
}
