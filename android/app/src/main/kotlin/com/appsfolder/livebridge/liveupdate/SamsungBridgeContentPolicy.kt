package ai.perplexity.app.android.liveupdate

internal data class SamsungBridgeTexts(
    val shouldClearContentText: Boolean,
    val secondaryText: String,
    val chipText: String?,
    val showSecondaryInNowBar: Boolean,
    val preferCompactNowBarRemoteView: Boolean
)

internal object SamsungBridgeContentPolicy {
    private const val YANDEX_MAPS_PACKAGE = "ru.yandex.yandexmaps"

    fun resolve(
        sourcePackageName: String,
        hasCustomRemoteCard: Boolean,
        hasProgress: Boolean,
        smartRuleId: String?,
        smartShortTextOverride: String?,
        displayText: String,
        compactPrimaryText: String,
        resolvedProgressChipText: String?,
        otpShortTextOverride: String?,
        otpCode: String?,
        compactCodeOverride: String?,
        samsungReparseChipText: String?
    ): SamsungBridgeTexts {
        val shouldClearContentText = hasCustomRemoteCard || smartRuleId == "navigation"
        val secondaryText = if (smartShortTextOverride != null && !hasProgress) {
            smartShortTextOverride
        } else {
            displayText
        }
        val chipText = sequenceOf(
            resolvedProgressChipText?.trim(),
            otpShortTextOverride?.trim(),
            otpCode?.trim(),
            compactCodeOverride?.trim(),
            samsungReparseChipText?.trim(),
            smartShortTextOverride?.trim(),
            compactPrimaryText.trim()
        ).firstOrNull { !it.isNullOrEmpty() }

        return SamsungBridgeTexts(
            shouldClearContentText = shouldClearContentText,
            secondaryText = secondaryText,
            chipText = chipText,
            showSecondaryInNowBar = smartRuleId != "navigation" && !hasCustomRemoteCard,
            preferCompactNowBarRemoteView = sourcePackageName == YANDEX_MAPS_PACKAGE && !hasProgress
        )
    }
}
