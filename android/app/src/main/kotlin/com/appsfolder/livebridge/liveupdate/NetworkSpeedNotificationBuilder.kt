package com.waymo.carapp.liveupdate

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.IconCompat
import com.waymo.carapp.MainActivity
import com.waymo.carapp.R

internal class NetworkSpeedNotificationBuilder(
    private val context: Context
) {
    fun build(
        prefs: ConverterPrefs,
        sample: NetworkSpeedSample
    ): Notification {
        val title = notificationTitle()
        val totalText = NetworkSpeedFormatter.totalText(sample, prefs)
        val contentText = NetworkSpeedFormatter.contentText(sample, prefs)
        val chipIconCompat = IconCompat.createWithResource(context, R.drawable.ic_speed)
        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_speed)
            .setContentTitle(title)
            .setContentText(contentText)
            .setContentIntent(contentIntent)
            .setColor(DEFAULT_ICON_ACCENT_COLOR)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
        builder.setRequestPromotedOngoing(true)
        builder.setShortCriticalText(totalText)
        if (SamsungLiveUpdateReparser.isSamsungDevice()) {
            builder.addExtras(
                buildSamsungExtras(
                    lockscreenOnly = prefs.getNetworkSpeedLockscreenOnly(),
                    chipBackgroundDisabled = prefs.getNetworkSpeedChipBackgroundDisabled(),
                    title = title,
                    chipText = totalText,
                    contentText = contentText,
                    chipIcon = chipIconCompat
                )
            )
        }

        return builder.build()
    }

    private fun buildSamsungExtras(
        lockscreenOnly: Boolean,
        chipBackgroundDisabled: Boolean,
        title: String,
        chipText: String,
        contentText: String,
        chipIcon: IconCompat
    ): Bundle {
        val icon = runCatching { chipIcon.toIcon(context) }.getOrNull()
        return Bundle().apply {
            putInt(KEY_STYLE, if (lockscreenOnly) STYLE_NOW_BAR_ONLY else STYLE_DEFAULT)
            putCharSequence(KEY_PRIMARY_INFO, title)
            putCharSequence(KEY_SECONDARY_INFO, contentText)
            putCharSequence(KEY_CHIP_EXPANDED_TEXT, chipText)
            putCharSequence(KEY_NOWBAR_PRIMARY_INFO, title)
            putCharSequence(KEY_NOWBAR_SECONDARY_INFO, contentText)
            putInt(KEY_CHIP_BG_COLOR, resolveChipBackgroundColor(chipBackgroundDisabled))
            putBoolean(KEY_SHOW_SMALL_ICON, true)
            icon?.let {
                putParcelable(KEY_CHIP_ICON, it)
                putParcelable(KEY_NOWBAR_ICON, it)
            }
        }
    }

    private fun resolveChipBackgroundColor(chipBackgroundDisabled: Boolean): Int {
        return if (chipBackgroundDisabled) {
            Color.TRANSPARENT
        } else {
            DEFAULT_CHIP_BG_COLOR
        }
    }

    private fun notificationTitle(): String = if (isRussianLocale()) TITLE_RU else TITLE_EN

    private fun isRussianLocale(): Boolean {
        val locale = context.resources.configuration.locales.get(0)
        return locale?.language?.startsWith("ru", ignoreCase = true) == true
    }

    companion object {
        const val CHANNEL_ID = "livebridge_network_speed"
        const val NOTIFICATION_ID = 41240

        private const val TITLE_EN = "Network speed"
        private const val TITLE_RU =
            "\u0421\u043a\u043e\u0440\u043e\u0441\u0442\u044c \u0438\u043d\u0442\u0435\u0440\u043d\u0435\u0442\u0430"

        private const val ONGOING_PREFIX = "android.ongoingActivityNoti."
        private const val KEY_STYLE = "${ONGOING_PREFIX}style"
        private const val KEY_PRIMARY_INFO = "${ONGOING_PREFIX}primaryInfo"
        private const val KEY_SECONDARY_INFO = "${ONGOING_PREFIX}secondaryInfo"
        private const val KEY_CHIP_BG_COLOR = "${ONGOING_PREFIX}chipBgColor"
        private const val KEY_CHIP_ICON = "${ONGOING_PREFIX}chipIcon"
        private const val KEY_CHIP_EXPANDED_TEXT = "${ONGOING_PREFIX}chipExpandedText"
        private const val KEY_NOWBAR_ICON = "${ONGOING_PREFIX}nowbarIcon"
        private const val KEY_NOWBAR_PRIMARY_INFO = "${ONGOING_PREFIX}nowbarPrimaryInfo"
        private const val KEY_NOWBAR_SECONDARY_INFO = "${ONGOING_PREFIX}nowbarSecondaryInfo"
        private const val KEY_SHOW_SMALL_ICON = "android.showSmallIcon"

        private const val DEFAULT_CHIP_BG_COLOR = 0xFF0F766E.toInt()
        private const val DEFAULT_ICON_ACCENT_COLOR = DEFAULT_CHIP_BG_COLOR
        private const val STYLE_DEFAULT = 1
        private const val STYLE_NOW_BAR_ONLY = 2
    }
}
