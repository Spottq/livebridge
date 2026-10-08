package com.kakao.taxi.liveupdate

/** Limit feedback loops without disabling conversion or cancelling unrelated notifications. */
internal class OriginalRemovalPolicy {
    private data class History(var first: Long, var count: Int, var blockedUntil: Long = 0)
    private val histories = linkedMapOf<String, History>()

    fun mayRemove(key: String, now: Long, persistent: Boolean): Boolean {
        if (persistent) return false
        val history = histories[key]
        if (history != null && now < history.blockedUntil) {
            history.blockedUntil = now + 60_000
            return false
        }
        val current = if (history == null || now - history.first >= 10_000) History(now, 0).also {
            histories[key] = it
        } else history
        if (current.count >= 2) {
            current.blockedUntil = now + 60_000
            return false
        }
        current.count++
        while (histories.size > 512) histories.remove(histories.keys.first())
        return true
    }
}
