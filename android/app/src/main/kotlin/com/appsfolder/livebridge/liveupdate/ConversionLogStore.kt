package com.kakao.taxi.liveupdate

import android.app.Notification
import android.content.Context
import android.service.notification.StatusBarNotification
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.Locale

object ConversionLogStore {
    private const val FILE_NAME = "conversion_log.json"
    private const val TAG = "ConversionLogStore"
    private const val CONTINUOUS_NOTIFICATION_UPDATE_WINDOW_MS = 2L * 60L * 1000L

    @Synchronized
    fun getEntriesRaw(context: Context): String {
        return readArray(context).toString()
    }

    @Synchronized
    fun getEntriesPageRaw(context: Context, offset: Int, limit: Int): String {
        val normalizedOffset = offset.coerceAtLeast(0)
        val normalizedLimit = limit.coerceIn(1, 100)
        val entries = readArray(context)
        val page = JSONArray()
        val endExclusive = minOf(entries.length(), normalizedOffset + normalizedLimit)

        if (normalizedOffset < entries.length()) {
            for (index in normalizedOffset until endExclusive) {
                entries.optJSONObject(index)?.let(page::put)
            }
        }

        return JSONObject().apply {
            put("entries", page)
            put("has_more", endExclusive < entries.length())
            put("total_count", entries.length())
        }.toString()
    }

    @Synchronized
    fun trimToPrefs(context: Context, prefs: ConverterPrefs) {
        val entries = readEntries(context)
        trimToMaxBytes(entries, prefs.getConversionLogMaxBytes())
        writeEntries(context, entries)
    }

    @Synchronized
    fun upsertMirroredNotification(
        context: Context,
        prefs: ConverterPrefs,
        sbn: StatusBarNotification,
        title: String,
        text: String
    ) {
        if (!prefs.getConversionLogEnabled()) {
            return
        }

        val entries = readEntries(context)
        val sourceKey = sbn.key
        val continuousSessionEntries = if (isContinuousNotification(sbn)) {
            findContinuousSessionEntries(entries, sourceKey, sbn.postTime)
        } else {
            emptyList()
        }
        val logKey = continuousSessionEntries.firstOrNull()?.logKey ?: buildLogKey(sbn)

        if (continuousSessionEntries.isNotEmpty()) {
            val staleLogKeys = continuousSessionEntries.mapTo(mutableSetOf()) { it.logKey }
            entries.removeAll { it.logKey in staleLogKeys }
        } else {
            entries.removeAll {
                it.logKey == logKey ||
                    (it.sourceKey == sourceKey && it.postedAtMs == sbn.postTime)
            }
        }

        entries.add(
            0,
            ConversionLogEntryRecord(
                logKey = logKey,
                sourceKey = sourceKey,
                packageName = sbn.packageName,
                appLabel = resolveAppLabel(context, sbn.packageName),
                postedAtMs = sbn.postTime,
                title = title,
                text = text,
                payloadJson = buildPayloadJson(
                    context = context,
                    sbn = sbn,
                    logKey = logKey,
                    title = title,
                    text = text
                )
            )
        )
        trimToMaxBytes(entries, prefs.getConversionLogMaxBytes())
        writeEntries(context, entries)
    }

    private fun findContinuousSessionEntries(
        entries: List<ConversionLogEntryRecord>,
        sourceKey: String,
        currentAtMs: Long
    ): List<ConversionLogEntryRecord> {
        val sessionEntries = mutableListOf<ConversionLogEntryRecord>()
        var newestAtMs = currentAtMs

        entries
            .asSequence()
            .filter { it.sourceKey == sourceKey }
            .forEach { entry ->
                if (!isWithinContinuousUpdateWindow(newestAtMs, entry.postedAtMs)) {
                    return sessionEntries
                }

                sessionEntries.add(entry)
                newestAtMs = entry.postedAtMs
            }

        return sessionEntries
    }

    private fun isWithinContinuousUpdateWindow(leftMs: Long, rightMs: Long): Boolean {
        if (leftMs <= 0L || rightMs <= 0L) {
            return true
        }
        val diffMs = if (leftMs >= rightMs) leftMs - rightMs else rightMs - leftMs
        return diffMs <= CONTINUOUS_NOTIFICATION_UPDATE_WINDOW_MS
    }

    private fun isContinuousNotification(sbn: StatusBarNotification): Boolean {
        val notification = sbn.notification
        val extras = notification.extras
        val hasProgress = extras.getInt(Notification.EXTRA_PROGRESS_MAX, 0) > 0 ||
            extras.getBoolean(Notification.EXTRA_PROGRESS_INDETERMINATE, false)

        return sbn.isOngoing ||
            hasProgress ||
            notification.category == Notification.CATEGORY_SERVICE ||
            notification.category == Notification.CATEGORY_PROGRESS ||
            notification.category == Notification.CATEGORY_STATUS ||
            notification.category == Notification.CATEGORY_TRANSPORT
    }

    private fun buildLogKey(sbn: StatusBarNotification): String {
        return "${sbn.key}|posted_at:${sbn.postTime}|event_at:${resolveEventTime(sbn)}"
    }

    private fun resolveEventTime(sbn: StatusBarNotification): Long {
        val notificationWhen = sbn.notification.`when`
        return if (notificationWhen > 0L) {
            notificationWhen
        } else {
            sbn.postTime
        }
    }

    private fun resolveAppLabel(context: Context, packageName: String): String {
        return try {
            val appInfo = context.packageManager.getApplicationInfo(packageName, 0)
            context.packageManager.getApplicationLabel(appInfo)?.toString()?.trim()
                .takeUnless { it.isNullOrBlank() } ?: packageName
        } catch (_: Throwable) {
            packageName
        }
    }

    private fun buildPayloadJson(
        context: Context,
        sbn: StatusBarNotification,
        logKey: String,
        title: String,
        text: String
    ): String {
        val notification = sbn.notification
        val extras = notification.extras
        val payload = JSONObject().apply {
            put("log_key", logKey)
            put("source_key", sbn.key)
            put("package_name", sbn.packageName)
            put("app_label", resolveAppLabel(context, sbn.packageName))
            put("posted_at_ms", sbn.postTime)
            put("notification_when_ms", resolveEventTime(sbn))
            put("notification_id", sbn.id)
            put("tag", sbn.tag)
            put("group_key", sbn.groupKey)
            put("channel_id", notification.channelId)
            put("title", title)
            put("text", text)
            put("is_clearable", sbn.isClearable)
            put("is_ongoing", sbn.isOngoing)
            put(
                "extras",
                JSONObject().apply {
                    put(
                        "title",
                        extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
                    )
                    put(
                        "title_big",
                        extras.getCharSequence(Notification.EXTRA_TITLE_BIG)?.toString()
                    )
                    put(
                        "text",
                        extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
                    )
                    put(
                        "big_text",
                        extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
                    )
                    put(
                        "sub_text",
                        extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()
                    )
                    put(
                        "summary_text",
                        extras.getCharSequence(Notification.EXTRA_SUMMARY_TEXT)?.toString()
                    )
                    put(
                        "info_text",
                        extras.getCharSequence(Notification.EXTRA_INFO_TEXT)?.toString()
                    )
                    val textLines = extras
                        .getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
                        ?.map { it?.toString() }
                        .orEmpty()
                    put("text_lines", JSONArray(textLines))
                }
            )
        }
        return payload.toString(2)
    }

    private fun readEntries(context: Context): MutableList<ConversionLogEntryRecord> {
        val rawEntries = readArray(context)
        val entries = mutableListOf<ConversionLogEntryRecord>()
        for (index in 0 until rawEntries.length()) {
            val item = rawEntries.optJSONObject(index) ?: continue
            ConversionLogEntryRecord.fromJson(item)?.let(entries::add)
        }
        return entries
    }

    private fun writeEntries(context: Context, entries: List<ConversionLogEntryRecord>) {
        val payload = JSONArray()
        entries.forEach { payload.put(it.toJson()) }
        fileFor(context).writeText(payload.toString(), StandardCharsets.UTF_8)
    }

    private fun trimToMaxBytes(entries: MutableList<ConversionLogEntryRecord>, maxBytes: Int) {
        while (entries.isNotEmpty() &&
            encodedSizeBytes(entries) > maxBytes
        ) {
            entries.removeLast()
        }
    }

    private fun encodedSizeBytes(entries: List<ConversionLogEntryRecord>): Int {
        val payload = JSONArray()
        entries.forEach { payload.put(it.toJson()) }
        return payload.toString().toByteArray(StandardCharsets.UTF_8).size
    }

    private fun readArray(context: Context): JSONArray {
        val file = fileFor(context)
        if (!file.exists()) {
            return JSONArray()
        }
        return try {
            JSONArray(file.readText(StandardCharsets.UTF_8))
        } catch (error: Throwable) {
            Log.e(TAG, "Failed to read conversion log", error)
            JSONArray()
        }
    }

    private fun fileFor(context: Context): File {
        return File(context.filesDir, FILE_NAME)
    }

    private data class ConversionLogEntryRecord(
        val logKey: String,
        val sourceKey: String,
        val packageName: String,
        val appLabel: String,
        val postedAtMs: Long,
        val title: String,
        val text: String,
        val payloadJson: String
    ) {
        fun toJson(): JSONObject {
            return JSONObject().apply {
                put("log_key", logKey)
                put("source_key", sourceKey)
                put("package_name", packageName.lowercase(Locale.ROOT))
                put("app_label", appLabel)
                put("posted_at_ms", postedAtMs)
                put("title", title)
                put("text", text)
                put("payload_json", payloadJson)
            }
        }

        companion object {
            fun fromJson(json: JSONObject): ConversionLogEntryRecord? {
                val sourceKey = json.optString("source_key").trim()
                val packageName = json.optString("package_name").trim()
                if (sourceKey.isBlank() || packageName.isBlank()) {
                    return null
                }
                val logKey = json.optString("log_key").trim().ifBlank { sourceKey }
                return ConversionLogEntryRecord(
                    logKey = logKey,
                    sourceKey = sourceKey,
                    packageName = packageName,
                    appLabel = json.optString("app_label").ifBlank { packageName },
                    postedAtMs = json.optLong("posted_at_ms"),
                    title = json.optString("title"),
                    text = json.optString("text"),
                    payloadJson = json.optString("payload_json")
                )
            }
        }
    }
}
