package com.kakao.taxi.liveupdate

/** Exact UTF-8 JSON array budget, including brackets and commas. Newest records come first. */
internal object LogRetention {
    fun retainedCount(encodedSizes: List<Int>, maxBytes: Int): Int {
        var bytes = 2L
        for ((index, size) in encodedSizes.withIndex()) {
            bytes += size.toLong() + if (index == 0) 0 else 1
            if (bytes > maxBytes) return index
        }
        return encodedSizes.size
    }
}
