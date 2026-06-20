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
        return totalText(sample, NetworkSpeedNotificationLocalizer.ENGLISH)
    }

    fun totalText(
        sample: NetworkSpeedSample,
        text: NetworkSpeedNotificationText
    ): String {
        return formatSpeedLine(sample.totalBytesPerSecond, text)
    }

    fun contentText(
        sample: NetworkSpeedSample,
        prefs: ConverterPrefs
    ): String {
        return contentText(sample, prefs, NetworkSpeedNotificationLocalizer.ENGLISH)
    }

    fun contentText(
        sample: NetworkSpeedSample,
        prefs: ConverterPrefs,
        text: NetworkSpeedNotificationText
    ): String {
        return contentText(sample, prefs) { bytesPerSecond ->
            formatSpeedLine(bytesPerSecond, text)
        }
    }

    fun regularNotificationContentText(
        sample: NetworkSpeedSample,
        prefs: ConverterPrefs
    ): String {
        return regularNotificationContentText(
            sample,
            prefs,
            NetworkSpeedNotificationLocalizer.ENGLISH
        )
    }

    fun regularNotificationContentText(
        sample: NetworkSpeedSample,
        prefs: ConverterPrefs,
        text: NetworkSpeedNotificationText
    ): String {
        return contentText(sample, prefs) { bytesPerSecond ->
            formatRegularNotificationSpeedLine(bytesPerSecond, text)
        }
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
        return formatSpeedLine(bytesPerSecond, NetworkSpeedNotificationLocalizer.ENGLISH)
    }

    private fun formatSpeedLine(
        bytesPerSecond: Long,
        text: NetworkSpeedNotificationText
    ): String {
        val (value, unit) = formatSpeedText(bytesPerSecond, text)
        return "$value$unit"
    }

    private fun formatRegularNotificationSpeedLine(
        bytesPerSecond: Long,
        text: NetworkSpeedNotificationText
    ): String {
        val (value, unit) = formatRegularNotificationSpeedText(bytesPerSecond, text)
        return "$value$unit"
    }

    fun statusIconText(sample: NetworkSpeedSample): Pair<String, String> {
        return formatStatusIconSpeedText(sample.totalBytesPerSecond)
    }

    private fun formatSpeedText(
        bytesPerSecond: Long,
        text: NetworkSpeedNotificationText
    ): Pair<String, String> {
        return formatSpeedText(
            bytesPerSecond = bytesPerSecond,
            unitToUse = resolveSpeedUnit(bytesPerSecond),
            text = text
        )
    }

    private fun formatRegularNotificationSpeedText(
        bytesPerSecond: Long,
        text: NetworkSpeedNotificationText
    ): Pair<String, String> {
        return when (resolveSpeedUnit(bytesPerSecond)) {
            SpeedUnit.KILOBYTES -> {
                "%.0f".format(
                    SPEED_NUMBER_LOCALE,
                    bytesPerSecond / KILOBYTE.toDouble()
                ) to text.kilobytesPerSecondUnit
            }

            SpeedUnit.MEGABYTES -> {
                "%.1f".format(
                    SPEED_NUMBER_LOCALE,
                    bytesPerSecond / MEGABYTE.toDouble()
                ) to text.megabytesPerSecondUnit
            }

            SpeedUnit.GIGABYTES -> {
                "%.1f".format(
                    SPEED_NUMBER_LOCALE,
                    bytesPerSecond / GIGABYTE.toDouble()
                ) to text.gigabytesPerSecondUnit
            }
        }
    }

    private fun formatStatusIconSpeedText(bytesPerSecond: Long): Pair<String, String> {
        return when (resolveSpeedUnit(bytesPerSecond)) {
            SpeedUnit.KILOBYTES -> {
                (bytesPerSecond / KILOBYTE.toDouble())
                    .roundToLong()
                    .coerceIn(0L, STATUS_ICON_MAX_VALUE)
                    .toString() to "KB/s"
            }

            SpeedUnit.MEGABYTES -> {
                formatStatusIconValue(bytesPerSecond / MEGABYTE.toDouble()) to "MB/s"
            }

            SpeedUnit.GIGABYTES -> {
                formatStatusIconValue(bytesPerSecond / GIGABYTE.toDouble()) to "GB/s"
            }
        }
    }

    private fun formatStatusIconValue(value: Double): String {
        if (value < 10.0) {
            val roundedTenths = (value * 10.0).roundToLong() / 10.0
            if (roundedTenths < 10.0) {
                return "%.1f".format(SPEED_NUMBER_LOCALE, roundedTenths)
            }
        }

        return value.roundToLong()
            .coerceIn(0L, STATUS_ICON_MAX_VALUE)
            .toString()
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
        unitToUse: SpeedUnit,
        text: NetworkSpeedNotificationText
    ): Pair<String, String> {
        return when (unitToUse) {
            SpeedUnit.KILOBYTES -> {
                formatFixedValue(
                    bytesPerSecond / KILOBYTE.toDouble(),
                    SPEED_NUMBER_LOCALE
                ) to text.kilobytesPerSecondUnit
            }

            SpeedUnit.MEGABYTES -> {
                val value = bytesPerSecond / MEGABYTE.toDouble()
                val valueText = if (value >= 10.0) {
                    value.roundToLong().toString()
                } else {
                    "%.1f".format(SPEED_NUMBER_LOCALE, value)
                }
                valueText to text.megabytesPerSecondUnit
            }

            SpeedUnit.GIGABYTES -> {
                formatFixedValue(
                    bytesPerSecond / GIGABYTE.toDouble(),
                    SPEED_NUMBER_LOCALE
                ) to text.gigabytesPerSecondUnit
            }
        }
    }

    private fun formatFixedValue(value: Double, locale: Locale): String {
        val pattern =
            when {
                value >= 100.0 -> "%.0f"
                value >= 10.0 -> "%.1f"
                else -> "%.2f"
            }
        return pattern.format(locale, value)
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
    private const val STATUS_ICON_MAX_VALUE = 999L
    private val SPEED_NUMBER_LOCALE = Locale.US
}
