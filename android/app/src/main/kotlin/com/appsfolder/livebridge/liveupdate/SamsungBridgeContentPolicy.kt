package com.kakao.taxi.liveupdate

internal data class SamsungBridgeTexts(
    val shouldClearContentText: Boolean,
    val secondaryText: String,
    val chipText: String?,
    val nowBarPrimaryText: String,
    val nowBarSecondaryText: String?,
    val showSecondaryInNowBar: Boolean,
    val preferCompactNowBarRemoteView: Boolean,
    val disableNowBarRemoteView: Boolean,
    val disableMiniRemoteView: Boolean,
    val showMiniIcon: Boolean,
    val showSmallIcon: Boolean,
    val allowNowBarProgress: Boolean,
    val keepCollapsedRemoteView: Boolean,
    val preferExpandedRemoteBody: Boolean,
    val reuseNotificationRemoteViews: Boolean
)

internal data class SamsungMiniTextPair(
    val primaryText: String,
    val secondaryText: String
)

internal object SamsungBridgeContentPolicy {
    private const val TWO_GIS_PACKAGE = "ru.dublgis.dgismobile"
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
        remoteViewMiniTextPair: SamsungMiniTextPair?,
        twoGisEtaDistanceText: String?
    ): SamsungBridgeTexts {
        val isTwoGisPackage = sourcePackageName == TWO_GIS_PACKAGE
        val useTextOnlyMiniNowBar =
            hasCustomRemoteCard && remoteViewMiniTextPair != null && !isTwoGisPackage
        val shouldClearContentText =
            !isTwoGisPackage &&
                    !useTextOnlyMiniNowBar &&
                    (hasCustomRemoteCard || smartRuleId == "navigation")
        val shouldUseSmartShortTextAsSecondary =
            !useTextOnlyMiniNowBar &&
                    smartShortTextOverride != null &&
                    !hasProgress &&
                    smartRuleId != "weather"
        val secondaryText = if (isTwoGisPackage) {
            twoGisEtaDistanceText?.trim()?.takeIf { it.isNotEmpty() } ?: displayText
        } else if (shouldUseSmartShortTextAsSecondary) {
            smartShortTextOverride
        } else {
            displayText
        }
        val chipText = sequenceOf(
            remoteViewMiniTextPair?.primaryText?.trim()?.takeIf { isTwoGisPackage },
            resolvedProgressChipText?.trim()?.takeIf { !isTwoGisPackage },
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
            isTwoGisPackage -> twoGisEtaDistanceText
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
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
                !useTextOnlyMiniNowBar && !isTwoGisPackage && (
                    sourcePackageName == YANDEX_MAPS_PACKAGE && !hasProgress
                ),
            disableNowBarRemoteView = isTwoGisPackage,
            disableMiniRemoteView = useTextOnlyMiniNowBar,
            showMiniIcon = !useTextOnlyMiniNowBar,
            showSmallIcon = !useTextOnlyMiniNowBar,
            allowNowBarProgress = !useTextOnlyMiniNowBar && !isTwoGisPackage,
            keepCollapsedRemoteView = useTextOnlyMiniNowBar,
            preferExpandedRemoteBody = false,
            reuseNotificationRemoteViews = !isTwoGisPackage
        )
    }
}
