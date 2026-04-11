package com.kakao.taxi.liveupdate.networkspeed

import java.util.Locale

object NetworkSpeedFormatter {
    fun formatCompact(bytesPerSecond: Long): String {
        val value = bytesPerSecond.coerceAtLeast(0L)
        return when {
            value < KILOBYTE -> "${value}B/s"
            value < MEGABYTE -> "${formatValue(value / KILOBYTE.toDouble())}K/s"
            value < GIGABYTE -> "${formatValue(value / MEGABYTE.toDouble())}M/s"
            else -> "${formatValue(value / GIGABYTE.toDouble())}G/s"
        }
    }

    private fun formatValue(value: Double): String {
        return when {
            value < 10 -> "%.1f".format(Locale.getDefault(), value)
            else -> "%.0f".format(Locale.getDefault(), value)
        }
    }

    private const val KILOBYTE = 1024L
    private const val MEGABYTE = 1024L * 1024L
    private const val GIGABYTE = 1024L * 1024L * 1024L
}
