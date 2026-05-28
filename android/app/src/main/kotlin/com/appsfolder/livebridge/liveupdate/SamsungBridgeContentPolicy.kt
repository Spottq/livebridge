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
    private const val YANGO_MAPS_PACKAGE = "com.yango.maps.android"

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
        twoGisPrimaryText: String?,
        twoGisEtaDistanceText: String?,
        twoGisVisibleSecondaryText: String?,
        preferSmartShortTextAsPrimary: Boolean = false
    ): SamsungBridgeTexts {
        val isVpnRule = smartRuleId == "vpn"
        val isTwoGisPackage = sourcePackageName == TWO_GIS_PACKAGE
        val isYandexMapsLikePackage =
            sourcePackageName == YANDEX_MAPS_PACKAGE ||
                    sourcePackageName == YANGO_MAPS_PACKAGE
        val preferredSmartText = smartShortTextOverride
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
        val useSmartShortTextAsPrimary =
            preferSmartShortTextAsPrimary && preferredSmartText != null
        val useTextOnlyMiniNowBar =
            !useSmartShortTextAsPrimary &&
                    hasCustomRemoteCard &&
                    remoteViewMiniTextPair != null &&
                    !isTwoGisPackage
        val shouldClearContentText =
            !useSmartShortTextAsPrimary &&
                    !isTwoGisPackage &&
                    !useTextOnlyMiniNowBar &&
                    (hasCustomRemoteCard || smartRuleId == "navigation")
        val shouldUseSmartShortTextAsSecondary =
            !useSmartShortTextAsPrimary &&
            !isTwoGisPackage &&
                    !useTextOnlyMiniNowBar &&
                    smartShortTextOverride != null &&
                    !hasProgress &&
                    !isVpnRule &&
                    smartRuleId != "weather"
        val secondaryText = if (useSmartShortTextAsPrimary) {
            displayText
        } else if (isTwoGisPackage) {
            twoGisVisibleSecondaryText?.trim()?.takeIf { it.isNotEmpty() }
                ?: twoGisEtaDistanceText?.trim()?.takeIf { it.isNotEmpty() }
                ?: displayText
        } else if (shouldUseSmartShortTextAsSecondary) {
            smartShortTextOverride
        } else {
            displayText
        }
        val chipText = if (useSmartShortTextAsPrimary) {
            preferredSmartText
        } else if (isVpnRule) {
            sequenceOf(
                smartShortTextOverride?.trim(),
                samsungReparseChipText?.trim(),
                compactPrimaryText.trim()
            ).firstOrNull { !it.isNullOrEmpty() }
        } else {
            sequenceOf(
                twoGisPrimaryText?.trim()?.takeIf { isTwoGisPackage && it.isNotEmpty() },
                resolvedProgressChipText?.trim()?.takeIf { !isTwoGisPackage },
                otpShortTextOverride?.trim(),
                otpCode?.trim(),
                compactCodeOverride?.trim(),
                samsungReparseChipText?.trim(),
                smartShortTextOverride?.trim(),
                compactPrimaryText.trim()
            ).firstOrNull { !it.isNullOrEmpty() }
        }
        val nowBarPrimaryText = if (useSmartShortTextAsPrimary) {
            preferredSmartText ?: compactPrimaryText.trim()
        } else if (isTwoGisPackage) {
            twoGisPrimaryText
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
                ?: remoteViewMiniTextPair?.primaryText
                    ?.trim()
                    ?.takeIf { it.isNotEmpty() }
                ?: compactPrimaryText.trim()
        } else {
            remoteViewMiniTextPair?.primaryText
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
                ?: compactPrimaryText.trim()
        }
        val showGenericSecondaryInNowBar =
            smartRuleId != "navigation" && !isVpnRule && !hasCustomRemoteCard
        val nowBarSecondaryText = when {
            useSmartShortTextAsPrimary -> null
            isTwoGisPackage -> twoGisVisibleSecondaryText
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
                ?: twoGisEtaDistanceText
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
            remoteViewMiniTextPair != null -> remoteViewMiniTextPair.secondaryText
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
            showGenericSecondaryInNowBar -> secondaryText
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
            showSecondaryInNowBar = !useSmartShortTextAsPrimary && (
                remoteViewMiniTextPair != null ||
                        showGenericSecondaryInNowBar
            ),
            preferCompactNowBarRemoteView =
                !useSmartShortTextAsPrimary &&
                !useTextOnlyMiniNowBar && !isTwoGisPackage && (
                    isYandexMapsLikePackage && !hasProgress
                ),
            disableNowBarRemoteView = useSmartShortTextAsPrimary || isTwoGisPackage,
            disableMiniRemoteView = useTextOnlyMiniNowBar,
            showMiniIcon = !useTextOnlyMiniNowBar,
            showSmallIcon = !useTextOnlyMiniNowBar,
            allowNowBarProgress = !useTextOnlyMiniNowBar && !isTwoGisPackage,
            keepCollapsedRemoteView = !useSmartShortTextAsPrimary && useTextOnlyMiniNowBar,
            preferExpandedRemoteBody = false,
            reuseNotificationRemoteViews = !useSmartShortTextAsPrimary && !isTwoGisPackage
        )
    }
}
