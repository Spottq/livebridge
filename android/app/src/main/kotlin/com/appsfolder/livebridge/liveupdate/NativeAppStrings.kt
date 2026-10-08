package com.kakao.taxi.liveupdate

import android.content.Context

internal object NativeAppStrings {
    fun language(context: Context): String = AppUiLanguage.resolve(
        ConverterPrefs(context).getAppLanguageTag(),
        context.resources.configuration.locales.get(0)?.language.orEmpty()
    )

    fun text(context: Context, en: String, ru: String = en, es: String = en, de: String = en): String =
        when (language(context)) {
            "ru" -> ru
            "es" -> es
            "de" -> de
            else -> en
        }
}
