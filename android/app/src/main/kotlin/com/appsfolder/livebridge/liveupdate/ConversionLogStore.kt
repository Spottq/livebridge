package com.kakao.taxi.liveupdate

import android.app.Notification
import android.content.Context
import android.service.notification.StatusBarNotification
import android.os.Handler
import android.os.HandlerThread
import android.os.Process
import android.util.AtomicFile
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

    private var cachedPath: String? = null
    private var cachedEntries: MutableList<ConversionLogEntryRecord>? = null
    private var flushScheduled = false
    private val diskHandler by lazy {
        HandlerThread("LiveBridge-log", Process.THREAD_PRIORITY_BACKGROUND).let {
            it.start()
            Handler(it.looper)
        }
    }

    @Synchronized
    fun getEntriesRaw(context: Context): String {
        return encodeEntries(readEntries(context))
    }

    @Synchronized
    fun getEntriesPageRaw(context: Context, offset: Int, limit: Int): String {
        val normalizedOffset = offset.coerceAtLeast(0)
        val normalizedLimit = limit.coerceIn(1, 100)
        val entries = readEntries(context)
        val page = JSONArray()
        val endExclusive = minOf(entries.size, normalizedOffset + normalizedLimit)

        if (normalizedOffset < entries.size) {
            for (index in normalizedOffset until endExclusive) {
                page.put(entries[index].toJson())
            }
        }

        return JSONObject().apply {
            put("entries", page)
            put("has_more", endExclusive < entries.size)
            put("total_count", entries.size)
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
        val appLabel = AppMetadataCache.get(context, sbn.packageName).label
        val record = ConversionLogEntryRecord(
            logKey = logKey,
            sourceKey = sourceKey,
            packageName = sbn.packageName,
            appLabel = appLabel,
            postedAtMs = sbn.postTime,
            title = title,
            text = text,
            payloadJson = buildPayloadJson(sbn, logKey, title, text, appLabel)
        )
        // Recovery/media polling must not write an identical log record again.
        if (entries.any { it == record }) return

        if (continuousSessionEntries.isNotEmpty()) {
            val staleLogKeys = continuousSessionEntries.mapTo(mutableSetOf()) { it.logKey }
            entries.removeAll { it.logKey in staleLogKeys }
        } else {
            entries.removeAll {
                it.logKey == logKey ||
                    (it.sourceKey == sourceKey && it.postedAtMs == sbn.postTime)
            }
        }

        entries.add(0, record)
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

    private fun buildPayloadJson(
        sbn: StatusBarNotification,
        logKey: String,
        title: String,
        text: String,
        appLabel: String
    ): String {
        val notification = sbn.notification
        val extras = notification.extras
        val payload = JSONObject().apply {
            put("log_key", logKey)
            put("source_key", sbn.key)
            put("package_name", sbn.packageName)
            put("app_label", appLabel)
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
        val path = fileFor(context).absolutePath
        if (cachedPath == path) cachedEntries?.let { return it }
        val entries = mutableListOf<ConversionLogEntryRecord>()
        val file = AtomicFile(fileFor(context))
        if (file.baseFile.exists()) {
            try {
                val raw = JSONArray(file.openRead().bufferedReader(StandardCharsets.UTF_8).use { it.readText() })
                for (index in 0 until raw.length()) {
                    raw.optJSONObject(index)?.let(ConversionLogEntryRecord::fromJson)?.let(entries::add)
                }
            } catch (error: Exception) {
                Log.e(TAG, "Failed to read conversion log", error)
            }
        }
        cachedPath = path
        cachedEntries = entries
        return entries
    }

    private fun encodeEntries(entries: List<ConversionLogEntryRecord>): String =
        entries.joinToString(separator = ",", prefix = "[", postfix = "]") { it.encodedJson }

    private fun writeEntries(context: Context, entries: List<ConversionLogEntryRecord>) {
        check(entries === cachedEntries)
        if (flushScheduled) return
        flushScheduled = true
        val appContext = context.applicationContext
        // Coalesce bursts, without delaying the first flush indefinitely under continuous updates.
        diskHandler.postDelayed({
            val snapshot = synchronized(this) {
                flushScheduled = false
                readEntries(appContext).toList()
            }
            // Records are immutable: encode outside the lock so pages/events can proceed.
            val payload = encodeEntries(snapshot)
            val file = AtomicFile(fileFor(appContext))
            var stream: java.io.FileOutputStream? = null
            try {
                stream = file.startWrite()
                stream.write(payload.toByteArray(StandardCharsets.UTF_8))
                file.finishWrite(stream)
            } catch (error: Exception) {
                file.failWrite(stream)
                Log.e(TAG, "Failed to persist conversion log", error)
            }
        }, 500L)
    }

    private fun trimToMaxBytes(entries: MutableList<ConversionLogEntryRecord>, maxBytes: Int) {
        val count = LogRetention.retainedCount(entries.map { it.encodedSizeBytes }, maxBytes)
        if (count < entries.size) entries.subList(count, entries.size).clear()
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
        val encodedJson: String by lazy { toJson().toString() }
        val encodedSizeBytes: Int by lazy { encodedJson.toByteArray(StandardCharsets.UTF_8).size }

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
