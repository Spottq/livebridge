package ai.perplexity.app.android.liveupdate

internal data class SamsungBridgeTexts(
    val shouldClearContentText: Boolean,
    val secondaryText: String,
    val chipText: String?,
    val nowBarPrimaryText: String,
    val nowBarSecondaryText: String?,
    val showSecondaryInNowBar: Boolean,
    val preferCompactNowBarRemoteView: Boolean
)

internal data class SamsungMiniTextPair(
    val primaryText: String,
    val secondaryText: String
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
        samsungReparseChipText: String?,
        remoteViewMiniTextPair: SamsungMiniTextPair?
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
        val nowBarPrimaryText = remoteViewMiniTextPair?.primaryText
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: compactPrimaryText.trim()
        val nowBarSecondaryText = when {
            remoteViewMiniTextPair != null -> remoteViewMiniTextPair.secondaryText
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
            smartRuleId != "navigation" && !hasCustomRemoteCard -> secondaryText
                .trim()
                .takeIf { it.isNotEmpty() }
            else -> null
        }

        return SamsungBridgeTexts(
            shouldClearContentText = shouldClearContentText,
            secondaryText = secondaryText,
            chipText = chipText,
            nowBarPrimaryText = nowBarPrimaryText,
            nowBarSecondaryText = nowBarSecondaryText,
            showSecondaryInNowBar =
                remoteViewMiniTextPair != null ||
                        (smartRuleId != "navigation" && !hasCustomRemoteCard),
            preferCompactNowBarRemoteView =
                sourcePackageName == YANDEX_MAPS_PACKAGE && !hasProgress
        )
    }
}
