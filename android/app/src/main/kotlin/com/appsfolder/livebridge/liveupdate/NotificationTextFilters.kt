package com.kakao.taxi.liveupdate

import org.json.JSONObject

internal object NotificationTextFilters {
    private var cachedRaw: String? = null
    private var policies: Map<String, TextFilterPolicy> = emptyMap()

    private var templates: Map<String, String> = emptyMap()

    private fun refresh(raw: String) {
        if (cachedRaw != raw) {
            val json = JSONObject(raw)
            policies = json.keys().asSequence().associateWith { key ->
                val rule = json.getJSONObject(key)
                fun terms(name: String): List<String> {
                    val list = rule.optJSONArray(name) ?: return emptyList()
                    return (0 until list.length()).map { list.getString(it).trim() }.filter { it.isNotEmpty() }
                }
                TextFilterPolicy(terms("allow"), terms("deny"), rule.optBoolean("all", false))
            }
            templates = json.keys().asSequence().associateWith { json.getJSONObject(it).optString("template", "") }
            cachedRaw = raw
        }
    }

    @Synchronized
    fun allows(raw: String, pkg: String, text: String): Boolean {
        refresh(raw)
        return (policies["*"]?.allows(text) != false) && (policies[pkg]?.allows(text) != false)
    }

    @Synchronized
    fun template(raw: String, pkg: String): String? {
        refresh(raw)
        return templates[pkg]?.takeIf { it.isNotBlank() } ?: templates["*"]?.takeIf { it.isNotBlank() }
    }
}
