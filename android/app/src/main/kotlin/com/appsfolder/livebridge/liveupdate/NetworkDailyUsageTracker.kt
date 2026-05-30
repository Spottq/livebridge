package com.kakao.taxi.liveupdate

import android.content.Context
import android.os.SystemClock
import org.json.JSONObject
import java.time.LocalDate
import java.util.Locale

internal data class NetworkDailyUsage(
    val wifiBytes: Long,
    val mobileBytes: Long
) {
    companion object {
        val ZERO = NetworkDailyUsage(wifiBytes = 0L, mobileBytes = 0L)
    }
}

internal class NetworkDailyUsageTracker(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private var day = prefs.getString(KEY_DAY, "").orEmpty()
    private var wifiBytes = prefs.getLong(KEY_WIFI_BYTES, 0L).coerceAtLeast(0L)
    private var mobileBytes = prefs.getLong(KEY_MOBILE_BYTES, 0L).coerceAtLeast(0L)
    private val counters = readCounters()
    private var lastPersistedAtElapsedMs = SystemClock.elapsedRealtime()

    @Synchronized
    fun start(snapshot: NetworkTrafficSnapshot): NetworkDailyUsage {
        val today = LocalDate.now().toString()
        if (day != today) {
            day = today
            wifiBytes = 0L
            mobileBytes = 0L
            counters.clear()
        }
        updateCounters(snapshot)
        persist()
        return currentUsage()
    }

    @Synchronized
    fun record(snapshot: NetworkTrafficSnapshot): NetworkDailyUsage {
        val today = LocalDate.now().toString()
        if (day != today) {
            day = today
            wifiBytes = 0L
            mobileBytes = 0L
            counters.clear()
            updateCounters(snapshot)
            persist()
            return currentUsage()
        }

        snapshot.interfaces.forEach { (name, current) ->
            val previous = counters[name]
            if (previous != null) {
                val delta =
                    positiveDelta(current.rxBytes, previous.rxBytes) +
                        positiveDelta(current.txBytes, previous.txBytes)
                when (current.transport) {
                    NetworkTransport.WIFI -> wifiBytes += delta
                    NetworkTransport.MOBILE -> mobileBytes += delta
                    NetworkTransport.ETHERNET -> Unit
                }
            }
        }
        updateCounters(snapshot)

        val nowElapsedMs = SystemClock.elapsedRealtime()
        if (nowElapsedMs - lastPersistedAtElapsedMs >= PERSIST_INTERVAL_MS) {
            persist()
        }
        return currentUsage()
    }

    @Synchronized
    fun flush() {
        persist()
    }

    private fun currentUsage(): NetworkDailyUsage {
        return NetworkDailyUsage(
            wifiBytes = wifiBytes.coerceAtLeast(0L),
            mobileBytes = mobileBytes.coerceAtLeast(0L)
        )
    }

    private fun updateCounters(snapshot: NetworkTrafficSnapshot) {
        snapshot.interfaces.forEach { (name, current) ->
            counters[name] = StoredCounter(
                rxBytes = current.rxBytes,
                txBytes = current.txBytes
            )
        }
    }

    private fun persist() {
        val rawCounters = JSONObject()
        counters.forEach { (name, counter) ->
            rawCounters.put(
                name,
                JSONObject()
                    .put(KEY_COUNTER_RX_BYTES, counter.rxBytes)
                    .put(KEY_COUNTER_TX_BYTES, counter.txBytes)
            )
        }
        prefs.edit()
            .putString(KEY_DAY, day)
            .putLong(KEY_WIFI_BYTES, wifiBytes)
            .putLong(KEY_MOBILE_BYTES, mobileBytes)
            .putString(KEY_COUNTERS, rawCounters.toString())
            .apply()
        lastPersistedAtElapsedMs = SystemClock.elapsedRealtime()
    }

    private fun readCounters(): MutableMap<String, StoredCounter> {
        val raw = prefs.getString(KEY_COUNTERS, null).orEmpty()
        if (raw.isBlank()) {
            return mutableMapOf()
        }
        return runCatching {
            val json = JSONObject(raw)
            buildMap {
                json.keys().forEach { name ->
                    val value = json.optJSONObject(name) ?: return@forEach
                    put(
                        name,
                        StoredCounter(
                            rxBytes = value.optLong(KEY_COUNTER_RX_BYTES, 0L),
                            txBytes = value.optLong(KEY_COUNTER_TX_BYTES, 0L)
                        )
                    )
                }
            }.toMutableMap()
        }.getOrDefault(mutableMapOf())
    }

    private fun positiveDelta(current: Long, previous: Long): Long {
        return (current - previous).coerceAtLeast(0L)
    }

    private data class StoredCounter(
        val rxBytes: Long,
        val txBytes: Long
    )

    companion object {
        private const val PREFS_NAME = "live_bridge_network_daily_usage"
        private const val KEY_DAY = "day"
        private const val KEY_WIFI_BYTES = "wifi_bytes"
        private const val KEY_MOBILE_BYTES = "mobile_bytes"
        private const val KEY_COUNTERS = "interface_counters"
        private const val KEY_COUNTER_RX_BYTES = "rx_bytes"
        private const val KEY_COUNTER_TX_BYTES = "tx_bytes"
        private const val PERSIST_INTERVAL_MS = 60_000L
    }
}

internal object NetworkDailyUsageFormatter {
    fun formatBytes(bytes: Long): String {
        val safeBytes = bytes.coerceAtLeast(0L)
        return if (safeBytes >= GIGABYTE) {
            formatValue(safeBytes / GIGABYTE.toDouble(), "Gb")
        } else {
            formatValue(safeBytes / MEGABYTE.toDouble(), "Mb")
        }
    }

    private fun formatValue(value: Double, suffix: String): String {
        if (value < 0.1) {
            return "0$suffix"
        }
        val pattern = if (value >= 10.0) "%.0f" else "%.1f"
        return pattern.format(Locale.US, value).removeSuffix(".0") + suffix
    }

    private const val MEGABYTE = 1024L * 1024L
    private const val GIGABYTE = 1024L * 1024L * 1024L
}
