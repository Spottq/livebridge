package com.kakao.taxi.liveupdate

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

internal enum class NotificationTitleSource(val id: String) {
    NOTIFICATION_TITLE("notification_title"),
    APP_TITLE("app_title");

    companion object {
        fun from(raw: String?): NotificationTitleSource? {
            val normalized = raw?.trim()?.lowercase(Locale.ROOT).orEmpty()
            return entries.firstOrNull { it.id == normalized }
        }
    }
}

internal enum class NotificationContentSource(val id: String) {
    NOTIFICATION_TEXT("notification_text"),
    NOTIFICATION_TITLE("notification_title");

    companion object {
        fun from(raw: String?): NotificationContentSource? {
            val normalized = raw?.trim()?.lowercase(Locale.ROOT).orEmpty()
            return entries.firstOrNull { it.id == normalized }
        }
    }
}

internal enum class NotificationIconSource(val id: String) {
    NOTIFICATION("notification"),
    APP("app");

    companion object {
        fun from(raw: String?): NotificationIconSource {
            val normalized = raw?.trim()?.lowercase(Locale.ROOT).orEmpty()
            return entries.firstOrNull { it.id == normalized } ?: APP
        }
    }
}

internal data class AppPresentationOverride(
    val compactTextSource: CompactTextSource = CompactTextSource.TITLE,
    val iconSource: NotificationIconSource = NotificationIconSource.APP,
    val titleSource: NotificationTitleSource? = null,
    val contentSource: NotificationContentSource? = null,
    val removeOriginalMessage: Boolean = false
) {
    fun usesExplicitSources(): Boolean {
        return titleSource != null || contentSource != null
    }

    fun resolvedTitleSource(): NotificationTitleSource {
        return titleSource ?: NotificationTitleSource.NOTIFICATION_TITLE
    }

    fun resolvedContentSource(): NotificationContentSource {
        return contentSource ?: NotificationContentSource.NOTIFICATION_TEXT
    }

    fun isDefault(): Boolean {
        return compactTextSource == CompactTextSource.TITLE &&
                iconSource == NotificationIconSource.APP &&
                resolvedTitleSource() == NotificationTitleSource.NOTIFICATION_TITLE &&
                resolvedContentSource() == NotificationContentSource.NOTIFICATION_TEXT &&
                !removeOriginalMessage
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
    private const val KEY_TITLE_SOURCE = "title_source"
    private const val KEY_CONTENT_SOURCE = "content_source"
    private const val KEY_REMOVE_ORIGINAL_MESSAGE = "remove_original_message"
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
        val titleSource = NotificationTitleSource.from(item.optString(KEY_TITLE_SOURCE))
        val contentSource = NotificationContentSource.from(item.optString(KEY_CONTENT_SOURCE))
        return AppPresentationOverride(
            compactTextSource = if (titleSource != null || contentSource != null) {
                CompactTextSource.TITLE
            } else {
                CompactTextSource.from(item.optString(KEY_COMPACT_TEXT))
            },
            iconSource = NotificationIconSource.from(item.optString(KEY_ICON_SOURCE)),
            titleSource = titleSource,
            contentSource = contentSource,
            removeOriginalMessage = item.optBoolean(KEY_REMOVE_ORIGINAL_MESSAGE, false)
        )
    }

    private fun encodeEntry(entry: AppPresentationOverride): JSONObject {
        return JSONObject().apply {
            put(KEY_ICON_SOURCE, entry.iconSource.id)
            if (entry.removeOriginalMessage) {
                put(KEY_REMOVE_ORIGINAL_MESSAGE, true)
            }
            if (entry.usesExplicitSources()) {
                put(KEY_TITLE_SOURCE, entry.resolvedTitleSource().id)
                put(KEY_CONTENT_SOURCE, entry.resolvedContentSource().id)
            } else {
                put(KEY_COMPACT_TEXT, entry.compactTextSource.id)
            }
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
