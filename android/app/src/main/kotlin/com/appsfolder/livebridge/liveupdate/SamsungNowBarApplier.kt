package com.kakao.taxi.liveupdate

import android.app.Notification
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.IconCompat

internal object SamsungNowBarApplier {
    fun apply(
        context: Context,
        builder: NotificationCompat.Builder,
        source: Notification,
        sourcePackageName: String,
        primaryText: String,
        texts: SamsungBridgeTexts,
        chipIcon: IconCompat?,
        nowBarIcon: IconCompat?,
        rightIcon: IconCompat?,
        suppressChipExpandedText: Boolean = false,
        suppressSourceRemoteViews: Boolean = false,
        suppressSourceNowBarRemoteView: Boolean = false,
        lockscreenOnly: Boolean,
        hasProgress: Boolean,
        progressValue: Int,
        progressMax: Int
    ) {
        if (texts.shouldClearContentText) {
            builder.setContentText("")
        }

        SamsungLiveUpdateReparser(context).applyNowBarBridge(
            builder = builder,
            source = source,
            sourcePackageName = sourcePackageName,
            primaryText = primaryText,
            secondaryText = texts.secondaryText,
            nowBarPrimaryText = texts.nowBarPrimaryText,
            nowBarSecondaryText = texts.nowBarSecondaryText,
            chipText = texts.chipText,
            chipIcon = chipIcon?.takeIf { texts.showMiniIcon },
            nowBarIcon = nowBarIcon?.takeIf { texts.showMiniIcon },
            rightIcon = rightIcon?.takeIf { texts.showMiniIcon },
            suppressChipExpandedText = suppressChipExpandedText,
            suppressSourceRemoteViews = suppressSourceRemoteViews,
            suppressSourceNowBarRemoteView = suppressSourceNowBarRemoteView,
            hasProgress = hasProgress,
            progressValue = progressValue,
            progressMax = progressMax,
            showSecondaryInNowBar = texts.showSecondaryInNowBar,
            preferCompactNowBarRemoteView = texts.preferCompactNowBarRemoteView,
            disableNowBarRemoteView = texts.disableNowBarRemoteView,
            disableMiniRemoteView = texts.disableMiniRemoteView,
            keepCollapsedRemoteView = texts.keepCollapsedRemoteView,
            preferExpandedRemoteBody = texts.preferExpandedRemoteBody,
            reuseNotificationRemoteViews = texts.reuseNotificationRemoteViews,
            showSmallIcon = texts.showSmallIcon,
            allowNowBarProgress = texts.allowNowBarProgress,
            lockscreenOnly = lockscreenOnly
        )
    }
}
