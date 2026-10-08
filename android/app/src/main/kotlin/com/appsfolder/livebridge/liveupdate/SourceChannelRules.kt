package com.kakao.taxi.liveupdate

import org.json.JSONObject

/** Cache parsed rules: notification processing must not decode JSON on every update. */
internal object SourceChannelRules {
    private var lastRaw: String? = null
    private var blocked: Map<String, Set<String>> = emptyMap()

    @Synchronized
    fun isAllowed(raw: String, packageName: String, channelId: String?): Boolean {
        if (raw != lastRaw) {
            val json = JSONObject(raw)
            blocked = json.keys().asSequence().associateWith { pkg ->
                val ids = json.getJSONArray(pkg)
                (0 until ids.length()).map { ids.getString(it) }.toSet()
            }
            lastRaw = raw
        }
        return channelAllowed(blocked, packageName, channelId)
    }
}

