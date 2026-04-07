package com.kakao.taxi.liveupdate

import android.content.Context
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

internal data class LiveParserDictionary(
    val smartRules: List<SmartRuleEntry>,
    val blockedSourcePackages: Set<String>,
    val knownNavigationPackages: Set<String>,
    val navigationPackageMarkers: Set<String>,
    val navigationDistancePattern: Regex,
    val otpStrongTriggers: Set<String>,
    val otpLooseTriggerPattern: Regex,
    val moneyContextPattern: Regex,
    val textProgressPercentPattern: Regex,
    val textProgressIncludeContextPattern: Regex,
    val textProgressExcludeContextPattern: Regex,
    val textProgressContextWindow: Int,
    val weatherPackageHints: Set<String>,
    val weatherContextPattern: Regex,
    val weatherTemperaturePattern: Regex,
    val weatherDayPattern: Regex,
    val weatherConditionPattern: Regex,
    val weatherConditionThunderPattern: Regex,
    val weatherConditionRainPattern: Regex,
    val weatherConditionSnowPattern: Regex,
    val weatherConditionFogPattern: Regex,
    val weatherConditionWindPattern: Regex,
    val weatherConditionSunPattern: Regex,
    val weatherConditionCloudPattern: Regex,
    val vpnSpeedPattern: Regex,
    val vpnContextPattern: Regex,
    val vpnPackageMarkers: Set<String>,
    val vpnDownloadMarkers: Set<String>,
    val vpnUploadMarkers: Set<String>,
    val externalDeviceNamePatterns: List<Regex>,
    val externalDeviceGenericNames: Set<String>,
    val navigationInstructionPattern: Regex,
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
                blockedSourcePackages = emptySet(),
                knownNavigationPackages = emptySet(),
                navigationPackageMarkers = emptySet(),
                navigationDistancePattern = emptyRegex,
                otpStrongTriggers = emptySet(),
                otpLooseTriggerPattern = emptyRegex,
                moneyContextPattern = emptyRegex,
                textProgressPercentPattern = emptyRegex,
                textProgressIncludeContextPattern = emptyRegex,
                textProgressExcludeContextPattern = emptyRegex,
                textProgressContextWindow = 80,
                weatherPackageHints = emptySet(),
                weatherContextPattern = emptyRegex,
                weatherTemperaturePattern = emptyRegex,
                weatherDayPattern = emptyRegex,
                weatherConditionPattern = emptyRegex,
                weatherConditionThunderPattern = emptyRegex,
                weatherConditionRainPattern = emptyRegex,
                weatherConditionSnowPattern = emptyRegex,
                weatherConditionFogPattern = emptyRegex,
                weatherConditionWindPattern = emptyRegex,
                weatherConditionSunPattern = emptyRegex,
                weatherConditionCloudPattern = emptyRegex,
                vpnSpeedPattern = emptyRegex,
                vpnContextPattern = emptyRegex,
                vpnPackageMarkers = emptySet(),
                vpnDownloadMarkers = emptySet(),
                vpnUploadMarkers = emptySet(),
                externalDeviceNamePatterns = emptyList(),
                externalDeviceGenericNames = emptySet(),
                navigationInstructionPattern = emptyRegex,
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
            val blockedSourcePackages =
                parseStringSet(root.optJSONArray("blocked_source_packages")).ifEmpty {
                    defaults.blockedSourcePackages
                }
            val knownNavigationPackages =
                parseStringSet(root.optJSONArray("known_navigation_packages")).ifEmpty {
                    defaults.knownNavigationPackages
                }
            val navigationPackageMarkers =
                parseStringSet(root.optJSONArray("navigation_package_markers")).ifEmpty {
                    defaults.navigationPackageMarkers
                }
            val navigationDistancePattern = parseRegex(
                root.optString("navigation_distance_pattern"),
                ignoreCase = true
            ) ?: defaults.navigationDistancePattern
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
            val textProgressPercentPattern = parseRegex(
                root.optString("text_progress_percent_pattern"),
                ignoreCase = true
            ) ?: defaults.textProgressPercentPattern
            val textProgressIncludeContextPattern = parseRegex(
                root.optString("text_progress_include_context_pattern"),
                ignoreCase = true
            ) ?: defaults.textProgressIncludeContextPattern
            val textProgressExcludeContextPattern = parseRegex(
                root.optString("text_progress_exclude_context_pattern"),
                ignoreCase = true
            ) ?: defaults.textProgressExcludeContextPattern
            val textProgressContextWindow =
                root.optInt("text_progress_context_window", defaults.textProgressContextWindow)
                    .coerceIn(24, 240)
            val weatherPackageHints =
                parseStringSet(root.optJSONArray("weather_package_hints")).ifEmpty {
                    defaults.weatherPackageHints
                }
            val weatherContextPattern = parseRegex(
                root.optString("weather_context_pattern"),
                ignoreCase = true
            ) ?: defaults.weatherContextPattern
            val parsedWeatherTemperaturePattern = parseRegex(
                root.optString("weather_temperature_pattern"),
                ignoreCase = true
            )
            val weatherTemperaturePattern = parsedWeatherTemperaturePattern
                ?.takeIf { it.containsMatchIn("1°") && it.containsMatchIn("-5°") }
                ?: defaults.weatherTemperaturePattern
            val weatherDayPattern = parseRegex(
                root.optString("weather_day_pattern"),
                ignoreCase = true
            ) ?: defaults.weatherDayPattern
            val weatherConditionPattern = parseRegex(
                root.optString("weather_condition_pattern"),
                ignoreCase = true
            ) ?: defaults.weatherConditionPattern
            val weatherConditionThunderPattern = parseRegex(
                root.optString("weather_condition_thunder_pattern"),
                ignoreCase = true
            ) ?: defaults.weatherConditionThunderPattern
            val weatherConditionRainPattern = parseRegex(
                root.optString("weather_condition_rain_pattern"),
                ignoreCase = true
            ) ?: defaults.weatherConditionRainPattern
            val weatherConditionSnowPattern = parseRegex(
                root.optString("weather_condition_snow_pattern"),
                ignoreCase = true
            ) ?: defaults.weatherConditionSnowPattern
            val weatherConditionFogPattern = parseRegex(
                root.optString("weather_condition_fog_pattern"),
                ignoreCase = true
            ) ?: defaults.weatherConditionFogPattern
            val weatherConditionWindPattern = parseRegex(
                root.optString("weather_condition_wind_pattern"),
                ignoreCase = true
            ) ?: defaults.weatherConditionWindPattern
            val weatherConditionSunPattern = parseRegex(
                root.optString("weather_condition_sun_pattern"),
                ignoreCase = true
            ) ?: defaults.weatherConditionSunPattern
            val weatherConditionCloudPattern = parseRegex(
                root.optString("weather_condition_cloud_pattern"),
                ignoreCase = true
            ) ?: defaults.weatherConditionCloudPattern
            val vpnSpeedPattern = parseRegex(
                root.optString("vpn_speed_pattern"),
                ignoreCase = true
            ) ?: defaults.vpnSpeedPattern
            val vpnContextPattern = parseRegex(
                root.optString("vpn_context_pattern"),
                ignoreCase = true
            ) ?: defaults.vpnContextPattern
            val vpnPackageMarkers =
                parseStringSet(root.optJSONArray("vpn_package_markers")).ifEmpty {
                    defaults.vpnPackageMarkers
                }
            val vpnDownloadMarkers =
                parseStringSet(root.optJSONArray("vpn_download_markers")).ifEmpty {
                    defaults.vpnDownloadMarkers
                }
            val vpnUploadMarkers =
                parseStringSet(root.optJSONArray("vpn_upload_markers")).ifEmpty {
                    defaults.vpnUploadMarkers
                }
            val externalDeviceNamePatterns =
                parseRegexList(root.optJSONArray("external_device_name_patterns"), ignoreCase = true).ifEmpty {
                    defaults.externalDeviceNamePatterns
                }
            val externalDeviceGenericNames =
                parseStringSet(root.optJSONArray("external_device_generic_names")).ifEmpty {
                    defaults.externalDeviceGenericNames
                }
            val navigationInstructionPattern = parseRegex(
                root.optString("navigation_instruction_pattern"),
                ignoreCase = true
            ) ?: defaults.navigationInstructionPattern

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
            val statusLabels = mergeStatusLabels(
                defaults = defaults.statusLabels,
                overrides = parseStatusLabels(root.optJSONObject("status_labels"))
            )

            return LiveParserDictionary(
                smartRules = smartRules,
                blockedSourcePackages = blockedSourcePackages,
                knownNavigationPackages = knownNavigationPackages,
                navigationPackageMarkers = navigationPackageMarkers,
                navigationDistancePattern = navigationDistancePattern,
                otpStrongTriggers = otpStrongTriggers,
                otpLooseTriggerPattern = otpLooseTriggerPattern,
                moneyContextPattern = moneyContextPattern,
                textProgressPercentPattern = textProgressPercentPattern,
                textProgressIncludeContextPattern = textProgressIncludeContextPattern,
                textProgressExcludeContextPattern = textProgressExcludeContextPattern,
                textProgressContextWindow = textProgressContextWindow,
                weatherPackageHints = weatherPackageHints,
                weatherContextPattern = weatherContextPattern,
                weatherTemperaturePattern = weatherTemperaturePattern,
                weatherDayPattern = weatherDayPattern,
                weatherConditionPattern = weatherConditionPattern,
                weatherConditionThunderPattern = weatherConditionThunderPattern,
                weatherConditionRainPattern = weatherConditionRainPattern,
                weatherConditionSnowPattern = weatherConditionSnowPattern,
                weatherConditionFogPattern = weatherConditionFogPattern,
                weatherConditionWindPattern = weatherConditionWindPattern,
                weatherConditionSunPattern = weatherConditionSunPattern,
                weatherConditionCloudPattern = weatherConditionCloudPattern,
                vpnSpeedPattern = vpnSpeedPattern,
                vpnContextPattern = vpnContextPattern,
                vpnPackageMarkers = vpnPackageMarkers,
                vpnDownloadMarkers = vpnDownloadMarkers,
                vpnUploadMarkers = vpnUploadMarkers,
                externalDeviceNamePatterns = externalDeviceNamePatterns,
                externalDeviceGenericNames = externalDeviceGenericNames,
                navigationInstructionPattern = navigationInstructionPattern,
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

        private fun mergeStatusLabels(
            defaults: Map<String, StageLabelsByLocale>,
            overrides: Map<String, StageLabelsByLocale>
        ): Map<String, StageLabelsByLocale> {
            if (defaults.isEmpty()) {
                return overrides
            }
            if (overrides.isEmpty()) {
                return defaults
            }

            val merged = defaults.toMutableMap()
            overrides.forEach { (ruleId, overrideLabels) ->
                val baseLabels = merged[ruleId]
                merged[ruleId] = if (baseLabels == null) {
                    overrideLabels
                } else {
                    StageLabelsByLocale(
                        ru = baseLabels.ru + overrideLabels.ru,
                        en = baseLabels.en + overrideLabels.en
                    )
                }
            }
            return merged
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
