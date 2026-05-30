package com.kakao.taxi.liveupdate

import java.util.Locale
import kotlin.math.roundToLong

internal data class NetworkSpeedSample(
    val downloadBytesPerSecond: Long,
    val uploadBytesPerSecond: Long
) {
    val totalBytesPerSecond: Long
        get() = downloadBytesPerSecond + uploadBytesPerSecond

    companion object {
        val ZERO = NetworkSpeedSample(downloadBytesPerSecond = 0L, uploadBytesPerSecond = 0L)
    }
}

internal object NetworkSpeedFormatter {
    fun totalText(sample: NetworkSpeedSample): String {
        return formatSpeedLine(sample.totalBytesPerSecond)
    }

    fun contentText(
        sample: NetworkSpeedSample,
        prefs: ConverterPrefs
    ): String {
        return contentText(sample, prefs, ::formatSpeedLine)
    }

    fun regularNotificationContentText(
        sample: NetworkSpeedSample,
        prefs: ConverterPrefs
    ): String {
        return contentText(sample, prefs, ::formatRegularNotificationSpeedLine)
    }

    private fun contentText(
        sample: NetworkSpeedSample,
        prefs: ConverterPrefs,
        formatLine: (Long) -> String
    ): String {
        val uploadText = UPLOAD_PREFIX + formatLine(sample.uploadBytesPerSecond)
        val downloadText = DOWNLOAD_PREFIX + formatLine(sample.downloadBytesPerSecond)

        return when (NetworkSpeedDisplayMode.from(prefs.getNetworkSpeedDisplayMode())) {
            NetworkSpeedDisplayMode.UPLOAD -> uploadText
            NetworkSpeedDisplayMode.DOWNLOAD -> downloadText
            NetworkSpeedDisplayMode.TOTAL -> {
                if (prefs.getNetworkSpeedPrioritizeUpload()) {
                    "$uploadText  $downloadText"
                } else {
                    "$downloadText  $uploadText"
                }
            }
        }
    }

    fun formatSpeedLine(bytesPerSecond: Long): String {
        val (value, unit) = formatSpeedText(bytesPerSecond)
        return "$value$unit"
    }

    private fun formatRegularNotificationSpeedLine(bytesPerSecond: Long): String {
        val (value, unit) = formatRegularNotificationSpeedText(bytesPerSecond)
        return "$value$unit"
    }

    fun statusIconText(sample: NetworkSpeedSample): Pair<String, String> {
        return formatRegularNotificationSpeedText(sample.totalBytesPerSecond)
    }

    private fun formatSpeedText(bytesPerSecond: Long): Pair<String, String> {
        return formatSpeedText(
            bytesPerSecond = bytesPerSecond,
            unitToUse = resolveSpeedUnit(bytesPerSecond)
        )
    }

    private fun formatRegularNotificationSpeedText(bytesPerSecond: Long): Pair<String, String> {
        return when (resolveSpeedUnit(bytesPerSecond)) {
            SpeedUnit.KILOBYTES -> {
                "%.0f".format(
                    Locale.getDefault(),
                    bytesPerSecond / KILOBYTE.toDouble()
                ) to "KB/s"
            }

            SpeedUnit.MEGABYTES -> {
                "%.1f".format(
                    Locale.getDefault(),
                    bytesPerSecond / MEGABYTE.toDouble()
                ) to "MB/s"
            }

            SpeedUnit.GIGABYTES -> {
                "%.1f".format(
                    Locale.getDefault(),
                    bytesPerSecond / GIGABYTE.toDouble()
                ) to "GB/s"
            }
        }
    }

    private fun resolveSpeedUnit(bytesPerSecond: Long): SpeedUnit {
        return when {
            bytesPerSecond >= GIGABYTE -> SpeedUnit.GIGABYTES
            bytesPerSecond >= MEGABYTE -> SpeedUnit.MEGABYTES
            else -> SpeedUnit.KILOBYTES
        }
    }

    private fun formatSpeedText(
        bytesPerSecond: Long,
        unitToUse: SpeedUnit
    ): Pair<String, String> {
        return when (unitToUse) {
            SpeedUnit.KILOBYTES -> {
                formatFixedValue(bytesPerSecond / KILOBYTE.toDouble()) to "KB/s"
            }

            SpeedUnit.MEGABYTES -> {
                val value = bytesPerSecond / MEGABYTE.toDouble()
                val text = if (value >= 10.0) {
                    value.roundToLong().toString()
                } else {
                    "%.1f".format(Locale.getDefault(), value)
                }
                text to "MB/s"
            }

            SpeedUnit.GIGABYTES -> {
                formatFixedValue(bytesPerSecond / GIGABYTE.toDouble()) to "GB/s"
            }
        }
    }

    private fun formatFixedValue(value: Double): String {
        val pattern =
            when {
                value >= 100.0 -> "%.0f"
                value >= 10.0 -> "%.1f"
                else -> "%.2f"
            }
        return pattern.format(Locale.getDefault(), value)
    }

    private enum class SpeedUnit {
        KILOBYTES,
        MEGABYTES,
        GIGABYTES
    }

    private const val UPLOAD_PREFIX = "\u25B2 "
    private const val DOWNLOAD_PREFIX = "\u25BC "
    private const val KILOBYTE = 1024L
    private const val MEGABYTE = 1024L * 1024L
    private const val GIGABYTE = 1024L * 1024L * 1024L
}
