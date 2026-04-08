package com.kakao.taxi.liveupdate

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.IconCompat
import com.kakao.taxi.MainActivity
import com.kakao.taxi.R

internal class NetworkSpeedNotificationBuilder(
    private val context: Context
) {
    fun build(
        prefs: ConverterPrefs,
        sample: NetworkSpeedSample,
        showLiveSurface: Boolean
    ): Notification {
        val totalText = NetworkSpeedFormatter.totalText(sample)
        val contentText = NetworkSpeedFormatter.contentText(sample, prefs)
        val chipIconCompat = IconCompat.createWithResource(context, R.drawable.ic_stat_liveupdate)
        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_liveupdate)
            .setContentTitle(notificationTitle())
            .setContentText(contentText)
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)

        if (showLiveSurface) {
            builder.setRequestPromotedOngoing(true)
            builder.setShortCriticalText(totalText)
            if (SamsungLiveUpdateReparser.isSamsungDevice()) {
                builder.addExtras(
                    buildSamsungExtras(
                        totalText = totalText,
                        contentText = contentText,
                        chipIcon = chipIconCompat
                    )
                )
            }
        }

        return builder.build()
    }

    private fun buildSamsungExtras(
        totalText: String,
        contentText: String,
        chipIcon: IconCompat
    ): Bundle {
        val icon = runCatching { chipIcon.toIcon(context) }.getOrNull()
        return Bundle().apply {
            putInt(KEY_STYLE, 1)
            putCharSequence(KEY_PRIMARY_INFO, totalText)
            putCharSequence(KEY_SECONDARY_INFO, contentText)
            putCharSequence(KEY_CHIP_EXPANDED_TEXT, totalText)
            putCharSequence(KEY_NOWBAR_PRIMARY_INFO, totalText)
            putCharSequence(KEY_NOWBAR_SECONDARY_INFO, contentText)
            putInt(KEY_CHIP_BG_COLOR, DEFAULT_CHIP_BG_COLOR)
            putBoolean(KEY_SHOW_SMALL_ICON, true)
            icon?.let {
                putParcelable(KEY_CHIP_ICON, it)
                putParcelable(KEY_NOWBAR_ICON, it)
            }
        }
    }

    private fun notificationTitle(): String {
        return if (isRussianLocale()) {
            "Скорость интернета"
        } else {
            "Network speed"
        }
    }

    private fun isRussianLocale(): Boolean {
        val locale = context.resources.configuration.locales.get(0)
        return locale?.language?.startsWith("ru", ignoreCase = true) == true
    }

    companion object {
        const val CHANNEL_ID = "livebridge_network_speed"
        const val NOTIFICATION_ID = 41240

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
    }
}
