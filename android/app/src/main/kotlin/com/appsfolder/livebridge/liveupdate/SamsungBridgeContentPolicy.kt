package ai.perplexity.app.android.liveupdate

internal data class SamsungBridgeTexts(
    val shouldClearContentText: Boolean,
    val secondaryText: String,
    val chipText: String?,
    val showSecondaryInNowBar: Boolean,
    val includeNowBarRemoteView: Boolean
)

internal object SamsungBridgeContentPolicy {
    fun resolve(
        hasCustomRemoteCard: Boolean,
        hasProgress: Boolean,
        smartShortTextOverride: String?,
        displayText: String,
        compactPrimaryText: String,
        compactSecondaryText: String?,
        compactChipText: String?,
        resolvedProgressChipText: String?,
        otpShortTextOverride: String?,
        otpCode: String?,
        compactCodeOverride: String?,
        samsungReparseChipText: String?
    ): SamsungBridgeTexts {
        val includeNowBarRemoteView = !hasCustomRemoteCard
        val secondaryText = compactSecondaryText
            ?: if (smartShortTextOverride != null && !hasProgress) {
                smartShortTextOverride
            } else {
                displayText
            }
        val chipText = sequenceOf(
            resolvedProgressChipText?.trim(),
            otpShortTextOverride?.trim(),
            otpCode?.trim(),
            compactCodeOverride?.trim(),
            compactChipText?.trim(),
            samsungReparseChipText?.trim(),
            smartShortTextOverride?.trim(),
            compactPrimaryText.trim()
        ).firstOrNull { !it.isNullOrEmpty() }

        return SamsungBridgeTexts(
            shouldClearContentText = false,
            secondaryText = secondaryText,
            chipText = chipText,
            showSecondaryInNowBar = secondaryText.isNotBlank(),
            includeNowBarRemoteView = includeNowBarRemoteView
        )
    }
}
