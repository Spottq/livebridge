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
        formatLine: (Long, String?) -> String
    ): String {
        val speedUnit = prefs.getNetworkSpeedUnit()
        val uploadText =
            prefs.getNetworkSpeedUploadPrefix() +
                formatLine(sample.uploadBytesPerSecond, speedUnit)
        val downloadText =
            prefs.getNetworkSpeedDownloadPrefix() +
                formatLine(sample.downloadBytesPerSecond, speedUnit)

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

    private fun formatRegularNotificationSpeedLine(
        bytesPerSecond: Long,
        rawUnit: String?
    ): String {
        val (value, unit) = formatRegularNotificationSpeedText(bytesPerSecond, rawUnit)
        return "$value$unit"
    }

    fun statusIconText(
        sample: NetworkSpeedSample,
        prefs: ConverterPrefs
    ): Pair<String, String> {
        return formatSpeedText(sample.totalBytesPerSecond, prefs.getNetworkSpeedUnit())
    }

    private fun formatSpeedText(
        bytesPerSecond: Long,
        rawUnit: String?
    ): Pair<String, String> {
        return formatSpeedText(
            bytesPerSecond = bytesPerSecond,
            unitToUse = resolveSpeedUnit(bytesPerSecond, rawUnit)
        )
    }

    private fun formatRegularNotificationSpeedText(
        bytesPerSecond: Long,
        rawUnit: String?
    ): Pair<String, String> {
        return when (val unitToUse = resolveSpeedUnit(bytesPerSecond, rawUnit)) {
            NetworkSpeedUnit.BYTES -> bytesPerSecond.toString() to "B/s"
            NetworkSpeedUnit.KILOBYTES -> {
                "%.0f".format(
                    Locale.getDefault(),
                    bytesPerSecond / KILOBYTE.toDouble()
                ) to "KB/s"
            }

            NetworkSpeedUnit.MEGABYTES -> {
                "%.1f".format(
                    Locale.getDefault(),
                    bytesPerSecond / MEGABYTE.toDouble()
                ) to "MB/s"
            }

            NetworkSpeedUnit.GIGABYTES -> {
                "%.1f".format(
                    Locale.getDefault(),
                    bytesPerSecond / GIGABYTE.toDouble()
                ) to "GB/s"
            }

            NetworkSpeedUnit.AUTO -> formatSpeedText(bytesPerSecond, unitToUse)
        }
    }

    private fun resolveSpeedUnit(
        bytesPerSecond: Long,
        rawUnit: String?
    ): NetworkSpeedUnit {
        val selectedUnits = NetworkSpeedUnit.parseSelection(rawUnit)
        val useAuto =
            selectedUnits.isEmpty() || selectedUnits.contains(NetworkSpeedUnit.AUTO)

        if (useAuto) {
            return when {
                bytesPerSecond >= GIGABYTE -> NetworkSpeedUnit.GIGABYTES
                bytesPerSecond >= MEGABYTE -> NetworkSpeedUnit.MEGABYTES
                bytesPerSecond >= KILOBYTE -> NetworkSpeedUnit.KILOBYTES
                else -> NetworkSpeedUnit.BYTES
            }
        }

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
        return best
    }

    private fun formatSpeedText(
        bytesPerSecond: Long,
        unitToUse: NetworkSpeedUnit
    ): Pair<String, String> {
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
