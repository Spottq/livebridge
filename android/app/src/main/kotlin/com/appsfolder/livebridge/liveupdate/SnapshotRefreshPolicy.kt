package com.kakao.taxi.liveupdate

/** Recovery polling must not rebuild static notifications on every tick. */
internal class SnapshotRefreshPolicy(private val reconciliationIntervalMs: Long = 60_000L) {
    private data class Stamp(val postTime: Long, val processedAt: Long)
    private val stamps = mutableMapOf<String, Stamp>()

    fun needsRefresh(key: String, postTime: Long, now: Long, periodic: Boolean): Boolean {
        val stamp = stamps[key] ?: return true
        return periodic || stamp.postTime != postTime ||
            now - stamp.processedAt >= reconciliationIntervalMs
    }

    fun record(key: String, postTime: Long, now: Long) {
        stamps[key] = Stamp(postTime, now)
    }

    fun remove(key: String) { stamps.remove(key) }
    fun retain(keys: Set<String>) { stamps.keys.retainAll(keys) }
    fun invalidate() { stamps.clear() }
}
