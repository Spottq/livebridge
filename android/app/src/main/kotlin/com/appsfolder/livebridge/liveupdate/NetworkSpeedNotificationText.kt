package com.kakao.taxi.liveupdate

import android.content.Context
import android.os.Build
import java.util.Locale

internal data class NetworkSpeedNotificationText(
    val numberLocale: Locale,
    val title: String,
    val channelDescription: String,
    val wifiLabel: String,
    val mobileLabel: String,
    val kilobytesPerSecondUnit: String,
    val megabytesPerSecondUnit: String,
    val gigabytesPerSecondUnit: String,
    val megabytesUnit: String,
    val gigabytesUnit: String
)

internal object NetworkSpeedNotificationLocalizer {
    val ENGLISH = NetworkSpeedNotificationText(
        numberLocale = Locale.ENGLISH,
        title = "Network speed",
        channelDescription = "Shows current network speed in the notification and Now Bar",
        wifiLabel = "Wi-Fi",
        mobileLabel = "Mobile",
        kilobytesPerSecondUnit = "KB/s",
        megabytesPerSecondUnit = "MB/s",
        gigabytesPerSecondUnit = "GB/s",
        megabytesUnit = "MB",
        gigabytesUnit = "GB"
    )

    fun resolve(context: Context, prefs: ConverterPrefs): NetworkSpeedNotificationText {
        return when (resolveLocale(context, prefs)) {
            LocaleKey.RU -> NetworkSpeedNotificationText(
                numberLocale = Locale("ru"),
                title = "\u0421\u043a\u043e\u0440\u043e\u0441\u0442\u044c \u0441\u0435\u0442\u0438",
                channelDescription =
                    "\u041f\u043e\u043a\u0430\u0437\u044b\u0432\u0430\u0435\u0442 " +
                        "\u0441\u043a\u043e\u0440\u043e\u0441\u0442\u044c " +
                        "\u0441\u0435\u0442\u0438 \u0432 " +
                        "\u0443\u0432\u0435\u0434\u043e\u043c\u043b\u0435\u043d\u0438\u0438 " +
                        "\u0438 Now Bar",
                wifiLabel = "Wi-Fi",
                mobileLabel = "\u041c\u043e\u0431",
                kilobytesPerSecondUnit = "\u043a\u0431/\u0441",
                megabytesPerSecondUnit = "\u043c\u0431/\u0441",
                gigabytesPerSecondUnit = "\u0433\u0431/\u0441",
                megabytesUnit = "\u041c\u0411",
                gigabytesUnit = "\u0413\u0411"
            )

            LocaleKey.TR -> NetworkSpeedNotificationText(
                numberLocale = Locale("tr"),
                title = "A\u011f h\u0131z\u0131",
                channelDescription =
                    "Ge\u00e7erli a\u011f h\u0131z\u0131n\u0131 bildirimde ve Now Bar'da g\u00f6sterir",
                wifiLabel = "Wi-Fi",
                mobileLabel = "Mobil",
                kilobytesPerSecondUnit = "KB/sn",
                megabytesPerSecondUnit = "MB/sn",
                gigabytesPerSecondUnit = "GB/sn",
                megabytesUnit = "MB",
                gigabytesUnit = "GB"
            )

            LocaleKey.PT_BR -> NetworkSpeedNotificationText(
                numberLocale = Locale("pt", "BR"),
                title = "Velocidade da rede",
                channelDescription =
                    "Mostra a velocidade atual da rede na notifica\u00e7\u00e3o e na Now Bar",
                wifiLabel = "Wi-Fi",
                mobileLabel = "M\u00f3vel",
                kilobytesPerSecondUnit = "KB/s",
                megabytesPerSecondUnit = "MB/s",
                gigabytesPerSecondUnit = "GB/s",
                megabytesUnit = "MB",
                gigabytesUnit = "GB"
            )

            LocaleKey.ZH_HANS -> NetworkSpeedNotificationText(
                numberLocale = Locale.forLanguageTag("zh-Hans"),
                title = "\u7f51\u901f",
                channelDescription =
                    "\u5728\u901a\u77e5\u548c Now Bar \u4e2d\u663e\u793a\u5f53\u524d\u7f51\u901f",
                wifiLabel = "Wi-Fi",
                mobileLabel = "\u79fb\u52a8",
                kilobytesPerSecondUnit = "KB/\u79d2",
                megabytesPerSecondUnit = "MB/\u79d2",
                gigabytesPerSecondUnit = "GB/\u79d2",
                megabytesUnit = "MB",
                gigabytesUnit = "GB"
            )

            LocaleKey.ZH_HANT -> NetworkSpeedNotificationText(
                numberLocale = Locale.forLanguageTag("zh-Hant"),
                title = "\u7db2\u901f",
                channelDescription =
                    "\u5728\u901a\u77e5\u548c Now Bar \u4e2d\u986f\u793a\u76ee\u524d\u7db2\u901f",
                wifiLabel = "Wi-Fi",
                mobileLabel = "\u884c\u52d5",
                kilobytesPerSecondUnit = "KB/\u79d2",
                megabytesPerSecondUnit = "MB/\u79d2",
                gigabytesPerSecondUnit = "GB/\u79d2",
                megabytesUnit = "MB",
                gigabytesUnit = "GB"
            )

            LocaleKey.KO -> NetworkSpeedNotificationText(
                numberLocale = Locale("ko"),
                title = "\ub124\ud2b8\uc6cc\ud06c \uc18d\ub3c4",
                channelDescription =
                    "\uc54c\ub9bc\uacfc Now Bar\uc5d0 \ud604\uc7ac " +
                        "\ub124\ud2b8\uc6cc\ud06c \uc18d\ub3c4\ub97c \ud45c\uc2dc\ud569\ub2c8\ub2e4",
                wifiLabel = "Wi-Fi",
                mobileLabel = "\ubaa8\ubc14\uc77c",
                kilobytesPerSecondUnit = "KB/\ucd08",
                megabytesPerSecondUnit = "MB/\ucd08",
                gigabytesPerSecondUnit = "GB/\ucd08",
                megabytesUnit = "MB",
                gigabytesUnit = "GB"
            )

            LocaleKey.EN -> ENGLISH
        }
    }

    private fun resolveLocale(context: Context, prefs: ConverterPrefs): LocaleKey {
        localeFromLanguageTag(prefs.getAppLanguageTag())?.let { return it }
        val locale = currentLocale(context) ?: return LocaleKey.EN
        return localeFromSystemLocale(locale)
    }

    private fun currentLocale(context: Context): Locale? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.resources.configuration.locales.get(0)
        } else {
            @Suppress("DEPRECATION")
            context.resources.configuration.locale
        }
    }

    private fun localeFromSystemLocale(locale: Locale): LocaleKey {
        val language = locale.language.lowercase(Locale.ROOT)
        return when {
            language.startsWith("ru") -> LocaleKey.RU
            language.startsWith("tr") -> LocaleKey.TR
            language.startsWith("pt") -> LocaleKey.PT_BR
            language.startsWith("zh") && isTraditionalChinese(locale) -> LocaleKey.ZH_HANT
            language.startsWith("zh") -> LocaleKey.ZH_HANS
            language.startsWith("ko") -> LocaleKey.KO
            else -> LocaleKey.EN
        }
    }

    private fun localeFromLanguageTag(languageTag: String?): LocaleKey? {
        val normalized = languageTag
            ?.trim()
            ?.replace('_', '-')
            ?.lowercase(Locale.ROOT)
            .orEmpty()
        return when {
            normalized.isBlank() || normalized == "system" -> null
            normalized.startsWith("ru") -> LocaleKey.RU
            normalized.startsWith("tr") -> LocaleKey.TR
            normalized.startsWith("pt") -> LocaleKey.PT_BR
            isTraditionalChineseLanguageTag(normalized) -> LocaleKey.ZH_HANT
            normalized.startsWith("zh") -> LocaleKey.ZH_HANS
            normalized.startsWith("ko") -> LocaleKey.KO
            else -> LocaleKey.EN
        }
    }

    private fun isTraditionalChineseLanguageTag(languageTag: String): Boolean {
        return languageTag.startsWith("zh") &&
            (
                languageTag.contains("hant") ||
                    languageTag.contains("-tw") ||
                    languageTag.contains("-hk") ||
                    languageTag.contains("-mo")
                )
    }

    private fun isTraditionalChinese(locale: Locale): Boolean {
        val script = locale.script.lowercase(Locale.ROOT)
        val country = locale.country.uppercase(Locale.ROOT)
        return script == "hant" || country in setOf("TW", "HK", "MO")
    }

    private enum class LocaleKey {
        EN,
        RU,
        TR,
        PT_BR,
        ZH_HANS,
        ZH_HANT,
        KO
    }
}
