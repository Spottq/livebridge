package ai.perplexity.app.android.liveupdate

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
            chipIcon = chipIcon,
            hasProgress = hasProgress,
            progressValue = progressValue,
            progressMax = progressMax,
            showSecondaryInNowBar = texts.showSecondaryInNowBar,
            preferCompactNowBarRemoteView = texts.preferCompactNowBarRemoteView
        )
    }
}
