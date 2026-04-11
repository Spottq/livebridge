package com.waymo.carapp.liveupdate

import org.json.JSONObject
import java.util.Locale

internal enum class CompactTextSource(val id: String) {
    TITLE("title"),
    TEXT("text");

    companion object {
        fun from(raw: String?): CompactTextSource {
            val normalized = raw?.trim()?.lowercase(Locale.ROOT).orEmpty()
            return entries.firstOrNull { it.id == normalized } ?: TITLE
        }
    }
}

internal enum class NotificationIconSource(val id: String) {
    NOTIFICATION("notification"),
    APP("app");

    companion object {
        fun from(raw: String?): NotificationIconSource {
            val normalized = raw?.trim()?.lowercase(Locale.ROOT).orEmpty()
            return entries.firstOrNull { it.id == normalized } ?: NOTIFICATION
        }
    }
}

internal data class AppPresentationOverride(
    val compactTextSource: CompactTextSource = CompactTextSource.TITLE,
    val iconSource: NotificationIconSource = NotificationIconSource.NOTIFICATION
) {
    fun isDefault(): Boolean {
        return compactTextSource == CompactTextSource.TITLE &&
                iconSource == NotificationIconSource.NOTIFICATION
    }
}

internal data class AppPresentationOverridesState(
    val defaultOverride: AppPresentationOverride = AppPresentationOverride(),
    val packageOverrides: Map<String, AppPresentationOverride> = emptyMap()
) {
    fun resolve(packageNameLower: String): AppPresentationOverride {
        return packageOverrides[packageNameLower] ?: defaultOverride
    }

    fun isEmpty(): Boolean {
        return defaultOverride.isDefault() && packageOverrides.isEmpty()
    }
}

internal object AppPresentationOverridesCodec {
    private const val KEY_COMPACT_TEXT = "compact_text"
    private const val KEY_ICON_SOURCE = "icon_source"
    private const val KEY_DEFAULT_OVERRIDE = "__default__"

    fun parse(raw: String?): AppPresentationOverridesState? {
        val normalized = raw?.trim().orEmpty()
        if (normalized.isEmpty()) {
            return AppPresentationOverridesState()
        }

        val root = try {
            JSONObject(normalized)
        } catch (_: Throwable) {
            return null
        }

        val defaultOverride = parseEntry(root.optJSONObject(KEY_DEFAULT_OVERRIDE))
            ?: AppPresentationOverride()
        val values = mutableMapOf<String, AppPresentationOverride>()
        val keys = root.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            if (key == KEY_DEFAULT_OVERRIDE) {
                continue
            }
            val packageName = key.trim().lowercase(Locale.ROOT)
            if (packageName.isEmpty()) {
                continue
            }
            val entry = parseEntry(root.optJSONObject(key)) ?: continue
            if (entry != defaultOverride) {
                values[packageName] = entry
            }
        }

        return AppPresentationOverridesState(
            defaultOverride = defaultOverride,
            packageOverrides = values
        )
    }

    fun encode(state: AppPresentationOverridesState): String {
        val root = JSONObject()
        if (!state.defaultOverride.isDefault()) {
            root.put(KEY_DEFAULT_OVERRIDE, encodeEntry(state.defaultOverride))
        }
        state.packageOverrides.toSortedMap().forEach { (packageName, entry) ->
            if (entry == state.defaultOverride) {
                return@forEach
            }
            root.put(packageName, encodeEntry(entry))
        }
        return root.toString()
    }

    fun normalizeForStorage(raw: String?): String? {
        val parsed = parse(raw) ?: return null
        return if (parsed.isEmpty()) "" else encode(parsed)
    }

    fun normalizeForDownload(raw: String?): String? {
        val parsed = parse(raw) ?: return null
        return encode(parsed)
    }

    private fun parseEntry(item: JSONObject?): AppPresentationOverride? {
        item ?: return null
        return AppPresentationOverride(
            compactTextSource = CompactTextSource.from(item.optString(KEY_COMPACT_TEXT)),
            iconSource = NotificationIconSource.from(item.optString(KEY_ICON_SOURCE))
        )
    }

    private fun encodeEntry(entry: AppPresentationOverride): JSONObject {
        return JSONObject().apply {
            put(KEY_COMPACT_TEXT, entry.compactTextSource.id)
            put(KEY_ICON_SOURCE, entry.iconSource.id)
        }
    }
}

internal object AppPresentationOverridesLoader {
    @Volatile
    private var cachedRaw: String? = null

    @Volatile
    private var cachedOverrides: AppPresentationOverridesState = AppPresentationOverridesState()

    fun get(prefs: ConverterPrefs): AppPresentationOverridesState {
        val raw = prefs.getAppPresentationOverridesRaw()
        cachedOverrides.let { existing ->
            if (cachedRaw == raw) {
                return existing
            }
        }

        synchronized(this) {
            cachedOverrides.let { existing ->
                if (cachedRaw == raw) {
                    return existing
                }
            }

            val parsed = AppPresentationOverridesCodec.parse(raw) ?: AppPresentationOverridesState()
            cachedRaw = raw
            cachedOverrides = parsed
            return parsed
        }
    }

    fun invalidate() {
        synchronized(this) {
            cachedRaw = null
            cachedOverrides = AppPresentationOverridesState()
        }
    }
}
