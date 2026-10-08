package com.kakao.taxi.liveupdate

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Metadata only, no notification content. Discovery does not invalidate converter settings. */
internal object SourceChannelStore {
    private const val MAX_CHANNELS = 512
    private var channels: LinkedHashMap<Pair<String, String>, String>? = null

    private fun load(context: Context): LinkedHashMap<Pair<String, String>, String> {
        channels?.let { return it }
        val result = linkedMapOf<Pair<String, String>, String>()
        val raw = context.getSharedPreferences("source_channel_catalog", Context.MODE_PRIVATE)
            .getString("channels", "[]") ?: "[]"
        try {
            val json = JSONArray(raw)
            for (i in 0 until minOf(json.length(), MAX_CHANNELS)) {
                val item = json.getJSONObject(i)
                result[item.getString("packageName") to item.getString("channelId")] = item.getString("name")
            }
        } catch (_: Exception) { /* A damaged catalog must not interrupt notification processing. */ }
        channels = result
        return result
    }

    @Synchronized
    fun observe(context: Context, pkg: String, id: String?, name: String?) {
        if (id.isNullOrEmpty() || pkg == context.packageName) return
        val items = load(context)
        val key = pkg to id
        val label = name?.takeIf { it.isNotBlank() } ?: items[key] ?: id
        if (items[key] == label) return
        items[key] = label
        while (items.size > MAX_CHANNELS) items.remove(items.keys.first())
        context.getSharedPreferences("source_channel_catalog", Context.MODE_PRIVATE).edit()
            .putString("channels", encode(items, emptyMap()).toString()).apply()
    }

    fun listJson(context: Context, prefs: ConverterPrefs): String {
        val items = synchronized(this) { LinkedHashMap(load(context)) }
        val json = JSONObject(prefs.getBlockedSourceChannelsRaw())
        val blocked = json.keys().asSequence().associateWith { pkg ->
            val ids = json.getJSONArray(pkg)
            (0 until ids.length()).map { ids.getString(it) }.toSet()
        }
        // Keep excluded channels visible even if their metadata was evicted or restored from backup.
        blocked.forEach { (pkg, ids) -> ids.forEach { id -> items.putIfAbsent(pkg to id, id) } }
        val encoded = encode(items, blocked)
        val labels = mutableMapOf<String, String>()
        for (i in 0 until encoded.length()) {
            val item = encoded.getJSONObject(i)
            val pkg = item.getString("packageName")
            val label = labels.getOrPut(pkg) {
                try {
                    val info = context.packageManager.getApplicationInfo(pkg, 0)
                    context.packageManager.getApplicationLabel(info).toString()
                } catch (_: Exception) { pkg }
            }
            item.put("appLabel", label)
        }
        return encoded.toString()
    }

    private fun encode(items: Map<Pair<String, String>, String>, blocked: Map<String, Set<String>>): JSONArray =
        JSONArray().apply {
            items.entries.sortedWith(compareBy({ it.key.first }, { it.value }, { it.key.second })).forEach { (key, name) ->
                put(JSONObject().put("packageName", key.first).put("channelId", key.second).put("name", name)
                    .put("enabled", channelAllowed(blocked, key.first, key.second)))
            }
        }
}
