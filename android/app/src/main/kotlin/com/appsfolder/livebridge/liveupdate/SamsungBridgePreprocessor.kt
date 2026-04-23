package com.kakao.taxi.liveupdate

import android.content.Context
import android.service.notification.StatusBarNotification

internal object SamsungBridgePreprocessor {
    fun build(
        context: Context,
        prefs: ConverterPrefs,
        sbn: StatusBarNotification,
        sourceHasNativeProgress: Boolean
    ): SamsungBridgeContext {
        val enabled = SamsungLiveUpdateReparser.isSamsungDevice()
        if (!enabled) {
            return SamsungBridgeContext.disabled(sourceHasNativeProgress)
        }

        val source = sbn.notification
        val reparsePayload = SamsungNotificationReparser.parse(context, sbn)
        val hasCustomRemoteCard =
            source.contentView != null ||
                    source.bigContentView != null ||
                    source.headsUpContentView != null

        return SamsungBridgeContext(
            enabled = true,
            reparsePayload = reparsePayload,
            hasNativeOrSamsungProgress =
                sourceHasNativeProgress || (reparsePayload?.hasProgress == true),
            hasCustomRemoteCard = hasCustomRemoteCard
        )
    }
}
