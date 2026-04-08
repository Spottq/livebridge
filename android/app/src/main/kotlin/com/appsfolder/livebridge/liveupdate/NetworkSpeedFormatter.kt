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
        val uploadText =
            prefs.getNetworkSpeedUploadPrefix() + formatSpeedLine(sample.uploadBytesPerSecond)
        val downloadText =
            prefs.getNetworkSpeedDownloadPrefix() + formatSpeedLine(sample.downloadBytesPerSecond)

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

    private fun formatFixedValue(value: Double): String {
        val pattern = when {
            value >= 100.0 -> "%.0f"
            value >= 10.0 -> "%.1f"
            else -> "%.2f"
        }
        return pattern.format(Locale.getDefault(), value)
    }

    fun formatSpeedLine(bytesPerSecond: Long): String {
        return when {
            bytesPerSecond >= GIGABYTE -> {
                formatFixedValue(bytesPerSecond / GIGABYTE.toDouble()) + "GB/s"
            }

            bytesPerSecond >= MEGABYTE -> {
                val value = bytesPerSecond / MEGABYTE.toDouble()
                val text = if (value >= 10.0) {
                    value.roundToLong().toString()
                } else {
                    "%.1f".format(Locale.getDefault(), value)
                }
                "${text}MB/s"
            }

            bytesPerSecond >= KILOBYTE -> {
                formatFixedValue(bytesPerSecond / KILOBYTE.toDouble()) + "KB/s"
            }

            else -> "${bytesPerSecond}B/s"
        }
    }

    private const val KILOBYTE = 1024L
    private const val MEGABYTE = 1024L * 1024L
    private const val GIGABYTE = 1024L * 1024L * 1024L
}
