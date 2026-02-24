package ai.perplexity.app.android.liveupdate

import android.content.Context
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

internal data class LiveParserDictionary(
    val smartRules: List<SmartRuleEntry>,
    val otpStrongTriggers: Set<String>,
    val otpLooseTriggerPattern: Regex,
    val moneyContextPattern: Regex,
    val otpCodePatterns: List<Regex>,
    val orderContextHints: Set<String>,
    val entityTokenPatterns: List<Regex>,
    val statusLabels: Map<String, StageLabelsByLocale>
) {
    fun resolveStatusText(ruleId: String, stageValue: Int, isRussianLocale: Boolean): String? {
        val labels = statusLabels[ruleId.lowercase(Locale.ROOT)] ?: return null
        return labels.resolve(stageValue, isRussianLocale)
    }

    companion object {
        fun default(): LiveParserDictionary {
            val emptyRegex = Regex("(?!)")
            return LiveParserDictionary(
                smartRules = emptyList(),
                otpStrongTriggers = emptySet(),
                otpLooseTriggerPattern = emptyRegex,
                moneyContextPattern = emptyRegex,
                otpCodePatterns = emptyList(),
                orderContextHints = emptySet(),
                entityTokenPatterns = emptyList(),
                statusLabels = emptyMap()
            )
        }

        fun fromJson(raw: String, defaults: LiveParserDictionary = default()): LiveParserDictionary? {
            val root = try {
                JSONObject(raw)
            } catch (_: Throwable) {
                return null
            }

            val smartRules = parseSmartRules(root.optJSONArray("smart_rules")) ?: defaults.smartRules
            val otpStrongTriggers =
                parseStringSet(root.optJSONArray("otp_strong_triggers")).ifEmpty { defaults.otpStrongTriggers }
            val otpLooseTriggerPattern = parseRegex(
                root.optString("otp_loose_trigger_pattern"),
                ignoreCase = true
            ) ?: defaults.otpLooseTriggerPattern
            val moneyContextPattern = parseRegex(
                root.optString("money_context_pattern"),
                ignoreCase = true
            ) ?: defaults.moneyContextPattern

            val otpCodePatterns =
                parseRegexList(root.optJSONArray("otp_code_patterns"), ignoreCase = false).ifEmpty {
                    defaults.otpCodePatterns
                }
            val orderContextHints =
                parseStringSet(root.optJSONArray("order_context_hints")).ifEmpty { defaults.orderContextHints }
            val entityTokenPatterns =
                parseRegexList(root.optJSONArray("entity_token_patterns"), ignoreCase = false).ifEmpty {
                    defaults.entityTokenPatterns
                }
            val statusLabels = parseStatusLabels(root.optJSONObject("status_labels")).ifEmpty { defaults.statusLabels }

            return LiveParserDictionary(
                smartRules = smartRules,
                otpStrongTriggers = otpStrongTriggers,
                otpLooseTriggerPattern = otpLooseTriggerPattern,
                moneyContextPattern = moneyContextPattern,
                otpCodePatterns = otpCodePatterns,
                orderContextHints = orderContextHints,
                entityTokenPatterns = entityTokenPatterns,
                statusLabels = statusLabels
            )
        }

        private fun parseSmartRules(raw: JSONArray?): List<SmartRuleEntry>? {
            raw ?: return null
            val parsed = mutableListOf<SmartRuleEntry>()

            for (index in 0 until raw.length()) {
                val item = raw.optJSONObject(index) ?: continue
                val id = item.optString("id").trim().lowercase(Locale.ROOT)
                if (id.isEmpty()) {
                    continue
                }

                val maxStage = item.optInt("max_stage", 1).coerceAtLeast(1)
                val packageHints = parseStringSet(item.optJSONArray("package_hints"))
                val textTriggers = parseStringSet(item.optJSONArray("text_triggers"))
                val excludePatterns = parseRegexList(item.optJSONArray("exclude_patterns"), ignoreCase = true)
                val signals = parseSignals(item.optJSONArray("signals"))

                if (signals.isEmpty()) {
                    continue
                }

                parsed += SmartRuleEntry(
                    id = id,
                    maxStage = maxStage,
                    packageHints = packageHints,
                    textTriggers = textTriggers,
                    excludePatterns = excludePatterns,
                    signals = signals
                )
            }

            return parsed.ifEmpty { null }
        }

        private fun parseSignals(raw: JSONArray?): List<SmartSignalEntry> {
            raw ?: return emptyList()
            val signals = mutableListOf<SmartSignalEntry>()
            for (index in 0 until raw.length()) {
                val item = raw.optJSONObject(index) ?: continue
                val stage = item.optInt("stage", Int.MIN_VALUE)
                if (stage == Int.MIN_VALUE) {
                    continue
                }
                val pattern = parseRegex(item.optString("pattern"), ignoreCase = true) ?: continue
                signals += SmartSignalEntry(stage = stage, pattern = pattern)
            }
            return signals
        }

        private fun parseStringSet(raw: JSONArray?): Set<String> {
            raw ?: return emptySet()
            val values = mutableSetOf<String>()
            for (index in 0 until raw.length()) {
                val value = raw.optString(index).trim().lowercase(Locale.ROOT)
                if (value.isNotEmpty()) {
                    values += value
                }
            }
            return values
        }

        private fun parseRegexList(raw: JSONArray?, ignoreCase: Boolean): List<Regex> {
            raw ?: return emptyList()
            val values = mutableListOf<Regex>()
            for (index in 0 until raw.length()) {
                parseRegex(raw.optString(index), ignoreCase)?.let(values::add)
            }
            return values
        }

        private fun parseStatusLabels(raw: JSONObject?): Map<String, StageLabelsByLocale> {
            raw ?: return emptyMap()
            val values = mutableMapOf<String, StageLabelsByLocale>()
            val keys = raw.keys()
            while (keys.hasNext()) {
                val ruleId = keys.next().trim().lowercase(Locale.ROOT)
                if (ruleId.isEmpty()) {
                    continue
                }
                val entry = raw.optJSONObject(ruleId) ?: continue
                val ru = parseStageLabelMap(entry.optJSONObject("ru"))
                val en = parseStageLabelMap(entry.optJSONObject("en"))
                if (ru.isEmpty() && en.isEmpty()) {
                    continue
                }
                values[ruleId] = StageLabelsByLocale(ru = ru, en = en)
            }
            return values
        }

        private fun parseStageLabelMap(raw: JSONObject?): Map<Int, String> {
            raw ?: return emptyMap()
            val values = mutableMapOf<Int, String>()
            val keys = raw.keys()
            while (keys.hasNext()) {
                val stageKey = keys.next()
                val stage = stageKey.toIntOrNull() ?: continue
                val value = raw.optString(stageKey).trim()
                if (value.isNotEmpty()) {
                    values[stage] = value
                }
            }
            return values.toSortedMap()
        }

        private fun parseRegex(value: String?, ignoreCase: Boolean): Regex? {
            val normalized = value?.trim().orEmpty()
            if (normalized.isEmpty()) {
                return null
            }
            return try {
                if (ignoreCase) {
                    Regex(normalized, setOf(RegexOption.IGNORE_CASE))
                } else {
                    Regex(normalized)
                }
            } catch (_: Throwable) {
                null
            }
        }
    }
}

internal data class SmartRuleEntry(
    val id: String,
    val maxStage: Int,
    val packageHints: Set<String>,
    val textTriggers: Set<String>,
    val excludePatterns: List<Regex> = emptyList(),
    val signals: List<SmartSignalEntry>
) {
    fun isRelevant(packageNameLower: String, textLower: String): Boolean {
        return packageHints.any(packageNameLower::contains) || textTriggers.any(textLower::contains)
    }

    fun isExcluded(textLower: String): Boolean {
        return excludePatterns.any { it.containsMatchIn(textLower) }
    }
}

internal data class SmartSignalEntry(
    val stage: Int,
    val pattern: Regex
)

internal data class StageLabelsByLocale(
    val ru: Map<Int, String>,
    val en: Map<Int, String>
) {
    fun resolve(stageValue: Int, isRussianLocale: Boolean): String? {
        val source = if (isRussianLocale) {
            if (ru.isNotEmpty()) ru else en
        } else {
            if (en.isNotEmpty()) en else ru
        }
        if (source.isEmpty()) {
            return null
        }

        source[stageValue]?.let { return it }
        val fallbackStage = source.keys.filter { it <= stageValue }.maxOrNull() ?: source.keys.minOrNull()
        return fallbackStage?.let(source::get)
    }
}

internal object LiveParserDictionaryLoader {
    private const val TAG = "LiveParserDictionary"
    private const val ASSET_FILE_NAME = "liveupdate_dictionary.json"
    private const val ASSET_CACHE_KEY = "__asset__"

    @Volatile
    private var cachedSourceKey: String? = null

    @Volatile
    private var cachedDictionary: LiveParserDictionary? = null

    fun get(context: Context, prefs: ConverterPrefs): LiveParserDictionary {
        val customRaw = prefs.getCustomParserDictionaryRaw()
        val sourceKey = customRaw ?: ASSET_CACHE_KEY
        cachedDictionary?.let { existing ->
            if (cachedSourceKey == sourceKey) {
                return existing
            }
        }
        synchronized(this) {
            cachedDictionary?.let { existing ->
                if (cachedSourceKey == sourceKey) {
                    return existing
                }
            }

            val bundledDictionary = loadFromAssets(context) ?: LiveParserDictionary.default()
            val loaded = if (!customRaw.isNullOrBlank()) {
                LiveParserDictionary.fromJson(customRaw, defaults = bundledDictionary)
                    ?: bundledDictionary
            } else {
                bundledDictionary
            }

            cachedSourceKey = sourceKey
            cachedDictionary = loaded
            return loaded
        }
    }

    fun invalidate() {
        synchronized(this) {
            cachedSourceKey = null
            cachedDictionary = null
        }
    }

    private fun loadFromAssets(context: Context): LiveParserDictionary? {
        return try {
            val raw = context.assets.open(ASSET_FILE_NAME).bufferedReader(Charsets.UTF_8).use { it.readText() }
            LiveParserDictionary.fromJson(raw)
        } catch (error: Throwable) {
            Log.w(TAG, "Failed to load parser dictionary from assets: ${error.message}")
            null
        }
    }
}
