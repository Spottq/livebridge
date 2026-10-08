package com.kakao.taxi.liveupdate

import java.security.MessageDigest

/** A dismissed snapshot stays dismissed; new content is a new notification, even with the same key. */
internal class NotificationRevisionPolicy {
    private val revisions = mutableMapOf<String, ByteArray>()
    private val aliases = mutableMapOf<String, Set<String>>()

    fun observe(key: String, content: String): Set<String> {
        val fingerprint = MessageDigest.getInstance("SHA-256").digest(content.toByteArray(Charsets.UTF_8))
        val previous = revisions.put(key, fingerprint)
        return if (previous != null && !previous.contentEquals(fingerprint)) aliases.remove(key).orEmpty() + key else emptySet()
    }

    fun dismissed(key: String, mirrorKeys: Set<String>) { aliases[key] = aliases[key].orEmpty() + mirrorKeys }
    fun remove(key: String): Set<String> { revisions.remove(key); return aliases.remove(key).orEmpty() }
    fun clear() { revisions.clear(); aliases.clear() }
}
