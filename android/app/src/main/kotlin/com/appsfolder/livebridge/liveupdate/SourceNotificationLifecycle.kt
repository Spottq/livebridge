package com.kakao.taxi.liveupdate

/** Worker-thread owned. Tracks keys only; never retains notification images/content. */
internal class SourceNotificationLifecycle {
    private val observed = mutableSetOf<String>()
    private val intentionallyRemoved = mutableSetOf<String>()

    fun posted(key: String) {
        observed.add(key)
        intentionallyRemoved.remove(key)
    }

    fun retainMirrorAfterRemoval(key: String) {
        observed.add(key)
        intentionallyRemoved.add(key)
    }

    fun removed(key: String): Boolean {
        if (key in intentionallyRemoved) return false
        observed.remove(key)
        return true
    }

    /** Call only with a successful, complete snapshot. */
    fun reconcile(activeKeys: Set<String>, retainedMirrorKeys: Set<String>): Set<String> {
        intentionallyRemoved.retainAll(retainedMirrorKeys)
        val missing = observed - activeKeys - intentionallyRemoved
        observed.removeAll(missing)
        observed.addAll(activeKeys)
        return missing
    }

    fun cancelRetention(key: String) { intentionallyRemoved.remove(key) }
}
