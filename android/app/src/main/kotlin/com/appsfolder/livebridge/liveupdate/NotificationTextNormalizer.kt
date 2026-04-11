package com.kakao.taxi.liveupdate

import java.nio.charset.Charset

internal object NotificationTextNormalizer {
    private val windows1251: Charset = Charset.forName("windows-1251")
    private val mojibakeHintPattern = Regex(
        "[Ѓѓ‚„†‡€‰Љ‹ЊЋЏђ‘’“”•–—™љ›њћџЎўёєіїґ€№]|[ÐÑÂÃ]"
    )

    fun normalize(value: CharSequence?): String? {
        val collapsed = collapseWhitespace(value?.toString().orEmpty())
        if (collapsed.isEmpty()) {
            return null
        }
        return repair(collapsed).ifBlank { null }
    }

    fun repair(value: String): String {
        val collapsed = collapseWhitespace(value)
        if (collapsed.isEmpty() || !mojibakeHintPattern.containsMatchIn(collapsed)) {
            return collapsed
        }

        return repairWithCharset(collapsed, windows1251)
            ?: repairWithCharset(collapsed, Charsets.ISO_8859_1)
            ?: collapsed
    }

    private fun repairWithCharset(value: String, sourceCharset: Charset): String? {
        val repaired = runCatching {
            String(value.toByteArray(sourceCharset), Charsets.UTF_8)
        }.getOrNull() ?: return null

        val collapsed = collapseWhitespace(repaired)
        if (collapsed.isEmpty() || collapsed == value || collapsed.contains(REPLACEMENT_CHAR)) {
            return null
        }

        val roundTrip = runCatching {
            String(collapsed.toByteArray(Charsets.UTF_8), sourceCharset)
        }.getOrNull() ?: return null

        return collapsed.takeIf { roundTrip == value }
    }

    private fun collapseWhitespace(value: String): String {
        return value.replace(Regex("\\s+"), " ").trim()
    }

    private const val REPLACEMENT_CHAR = '\uFFFD'
}
