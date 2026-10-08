package com.kakao.taxi.liveupdate

internal data class TextFilterPolicy(val allow: List<String> = emptyList(), val deny: List<String> = emptyList(), val all: Boolean = false) {
    fun allows(text: String): Boolean {
        if (deny.any { text.contains(it, ignoreCase = true) }) return false
        if (allow.isEmpty()) return true
        return if (all) allow.all { text.contains(it, ignoreCase = true) } else allow.any { text.contains(it, ignoreCase = true) }
    }
}
