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
    fun totalText(sample: NetworkSpeedSample, prefs: ConverterPrefs): String {
        return formatSpeedLine(sample.totalBytesPerSecond, prefs.getNetworkSpeedUnit())
    }

    fun contentText(
        sample: NetworkSpeedSample,
        prefs: ConverterPrefs
    ): String {
        val speedUnit = prefs.getNetworkSpeedUnit()
        val uploadText =
            prefs.getNetworkSpeedUploadPrefix() +
                formatSpeedLine(sample.uploadBytesPerSecond, speedUnit)
        val downloadText =
            prefs.getNetworkSpeedDownloadPrefix() +
                formatSpeedLine(sample.downloadBytesPerSecond, speedUnit)

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

    fun formatSpeedLine(
        bytesPerSecond: Long,
        rawUnit: String?
    ): String {
        val (value, unit) = formatSpeedText(bytesPerSecond, rawUnit)
        return "$value$unit"
    }

    private fun formatSpeedText(
        bytesPerSecond: Long,
        rawUnit: String?
    ): Pair<String, String> {
        val selectedUnits = NetworkSpeedUnit.parseSelection(rawUnit)
        val useAuto =
            selectedUnits.isEmpty() || selectedUnits.contains(NetworkSpeedUnit.AUTO)

        val unitToUse =
            if (useAuto) {
                when {
                    bytesPerSecond >= GIGABYTE -> NetworkSpeedUnit.GIGABYTES
                    bytesPerSecond >= MEGABYTE -> NetworkSpeedUnit.MEGABYTES
                    bytesPerSecond >= KILOBYTE -> NetworkSpeedUnit.KILOBYTES
                    else -> NetworkSpeedUnit.BYTES
                }
            } else {
                val sortedAvailable = selectedUnits
                    .filter { it != NetworkSpeedUnit.AUTO }
                    .sortedByDescending { it.ordinal }
                var best = sortedAvailable.last()
                for (unit in sortedAvailable) {
                    val threshold =
                        when (unit) {
                            NetworkSpeedUnit.GIGABYTES -> 1000L * 1024L * 1024L
                            NetworkSpeedUnit.MEGABYTES -> 1000L * 1024L
                            NetworkSpeedUnit.KILOBYTES -> KILOBYTE
                            NetworkSpeedUnit.BYTES,
                            NetworkSpeedUnit.AUTO -> 0L
                        }
                    if (bytesPerSecond >= threshold) {
                        best = unit
                        break
                    }
                }
                best
            }

        return when (unitToUse) {
            NetworkSpeedUnit.BYTES -> bytesPerSecond.toString() to "B/s"
            NetworkSpeedUnit.KILOBYTES -> {
                formatFixedValue(bytesPerSecond / KILOBYTE.toDouble()) to "KB/s"
            }

            NetworkSpeedUnit.MEGABYTES -> {
                val value = bytesPerSecond / MEGABYTE.toDouble()
                val text = if (value >= 10.0) {
                    value.roundToLong().toString()
                } else {
                    "%.1f".format(Locale.getDefault(), value)
                }
                text to "MB/s"
            }

            NetworkSpeedUnit.GIGABYTES -> {
                formatFixedValue(bytesPerSecond / GIGABYTE.toDouble()) to "GB/s"
            }

            NetworkSpeedUnit.AUTO -> bytesPerSecond.toString() to "B/s"
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

    private const val KILOBYTE = 1024L
    private const val MEGABYTE = 1024L * 1024L
    private const val GIGABYTE = 1024L * 1024L * 1024L
}
