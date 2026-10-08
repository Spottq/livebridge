package com.kakao.taxi.liveupdate

private val templateTokens = Regex("\\{(title|text|app)\\}")

internal fun renderNotificationTemplate(template: String?, title: String, text: String, app: String): String? {
    if (template.isNullOrBlank()) return null
    return templateTokens.replace(template) { match ->
        when (match.groupValues[1]) { "title" -> title; "text" -> text; else -> app }
    }.trim().takeIf { it.isNotEmpty() }
}
