package com.kakao.taxi.liveupdate

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Bundle
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.IconCompat
import com.kakao.taxi.MainActivity
import com.kakao.taxi.R
import kotlin.math.roundToInt

internal class NetworkSpeedNotificationBuilder(
    private val context: Context
) {
    fun build(
        prefs: ConverterPrefs,
        sample: NetworkSpeedSample,
        dailyUsage: NetworkDailyUsage
    ): Notification {
        val title = notificationTitle()
        val totalText = NetworkSpeedFormatter.totalText(sample)
        val contentText = NetworkSpeedFormatter.contentText(sample, prefs)
        val regularContentText =
            NetworkSpeedFormatter.regularNotificationContentText(sample, prefs)
        val dailyUsageText =
            if (prefs.getNetworkSpeedDailyUsageEnabled()) {
                buildDailyUsageText(dailyUsage)
            } else {
                null
            }
        val liveContentText =
            if (dailyUsageText == null) {
                contentText
            } else {
                "$contentText\n$dailyUsageText"
            }
        val regularNotificationText =
            if (dailyUsageText == null) {
                regularContentText
            } else {
                "$regularContentText\n$dailyUsageText"
            }
        val notificationColor = prefs.getNetworkSpeedNotificationColorArgb()
        val regularNotificationOnly = prefs.getNetworkSpeedRegularNotificationEnabled()
        val shouldPromote =
            !regularNotificationOnly &&
                sample.totalBytesPerSecond >=
                prefs.getNetworkSpeedMinThresholdBytesPerSecond().coerceAtLeast(0L)
        val chipIconCompat = IconCompat.createWithResource(context, R.drawable.ic_speed)
        val samsungNowBarEligible =
            shouldPromote && SamsungLiveUpdateReparser.isSamsungDevice()
        val notificationSmallIcon =
            if (samsungNowBarEligible) {
                chipIconCompat
            } else {
                buildStatusIcon(sample)
            }
        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(notificationSmallIcon)
            .setContentTitle(title)
            .setContentText(regularNotificationText)
            .setContentIntent(contentIntent)
            .setColor(notificationColor)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
        if (dailyUsageText != null) {
            builder.setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(regularNotificationText)
            )
        }
        if (shouldPromote) {
            builder.setRequestPromotedOngoing(true)
            builder.setShortCriticalText(totalText)
        }
        if (samsungNowBarEligible) {
            builder.addExtras(
                buildSamsungExtras(
                    chipBackgroundDisabled = prefs.getNetworkSpeedChipBackgroundDisabled(),
                    chipBackgroundColor = notificationColor,
                    title = title,
                    chipText = totalText,
                    contentText = liveContentText,
                    chipIcon = chipIconCompat
                )
            )
        }

        val notification = builder.build()
        return if (samsungNowBarEligible) {
            SamsungOneUi7NowBarCompat.markEligible(notification)
        } else {
            notification
        }
    }

    private fun buildDailyUsageText(dailyUsage: NetworkDailyUsage): String {
        val wifiText = NetworkDailyUsageFormatter.formatBytes(dailyUsage.wifiBytes)
        val mobileText = NetworkDailyUsageFormatter.formatBytes(dailyUsage.mobileBytes)
        return "WiFi: $wifiText  Mobile: $mobileText"
    }

    private fun buildSamsungExtras(
        chipBackgroundDisabled: Boolean,
        chipBackgroundColor: Int,
        title: String,
        chipText: String,
        contentText: String,
        chipIcon: IconCompat
    ): Bundle {
        val icon = runCatching { chipIcon.toIcon(context) }.getOrNull()
        return Bundle().apply {
            putInt(KEY_STYLE, STYLE_DEFAULT)
            putCharSequence(KEY_PRIMARY_INFO, title)
            putCharSequence(KEY_SECONDARY_INFO, contentText)
            putCharSequence(KEY_CHIP_EXPANDED_TEXT, chipText)
            putCharSequence(KEY_NOWBAR_PRIMARY_INFO, title)
            putCharSequence(KEY_NOWBAR_SECONDARY_INFO, contentText)
            putInt(
                KEY_CHIP_BG_COLOR,
                resolveChipBackgroundColor(chipBackgroundDisabled, chipBackgroundColor)
            )
            putBoolean(KEY_SHOW_SMALL_ICON, true)
            icon?.let {
                putParcelable(KEY_CHIP_ICON, it)
                putParcelable(KEY_NOWBAR_ICON, it)
            }
        }
    }

    private fun resolveChipBackgroundColor(
        chipBackgroundDisabled: Boolean,
        chipBackgroundColor: Int
    ): Int {
        return if (chipBackgroundDisabled) {
            Color.TRANSPARENT
        } else {
            chipBackgroundColor
        }
    }

    private fun buildStatusIcon(sample: NetworkSpeedSample): IconCompat {
        val size = (context.resources.displayMetrics.density * STATUS_ICON_DP)
            .roundToInt()
            .coerceAtLeast(STATUS_ICON_MIN_PX)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val (valueText, unitText) = NetworkSpeedFormatter.statusIconText(sample)
        val maxTextWidth = size * STATUS_ICON_MAX_WIDTH_FACTOR
        val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
            textScaleX = STATUS_ICON_VALUE_TEXT_SCALE_X
            fitTextSize(valueText, size * STATUS_ICON_VALUE_TEXT_FACTOR, maxTextWidth)
        }
        val unitPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
            textScaleX = STATUS_ICON_UNIT_TEXT_SCALE_X
            fitTextSize(unitText, size * STATUS_ICON_UNIT_TEXT_FACTOR, maxTextWidth)
        }

        drawCenteredText(canvas, valueText, size * STATUS_ICON_VALUE_CENTER_Y_FACTOR, valuePaint)
        drawCenteredText(canvas, unitText, size * STATUS_ICON_UNIT_CENTER_Y_FACTOR, unitPaint)
        return IconCompat.createWithBitmap(bitmap)
    }

    private fun Paint.fitTextSize(
        text: String,
        preferredSize: Float,
        maxWidth: Float
    ) {
        var nextSize = preferredSize
        textSize = nextSize
        while (nextSize > STATUS_ICON_MIN_TEXT_SIZE && measureText(text) > maxWidth) {
            nextSize -= STATUS_ICON_TEXT_SHRINK_STEP
            textSize = nextSize
        }
    }

    private fun drawCenteredText(
        canvas: Canvas,
        text: String,
        centerY: Float,
        paint: Paint
    ) {
        val metrics = paint.fontMetrics
        val baseline = centerY - (metrics.ascent + metrics.descent) / 2f
        canvas.drawText(text, canvas.width / 2f, baseline, paint)
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

        private const val STATUS_ICON_DP = 64f
        private const val STATUS_ICON_MIN_PX = 128
        private const val STATUS_ICON_MAX_WIDTH_FACTOR = 0.98f
        private const val STATUS_ICON_VALUE_TEXT_FACTOR = 0.82f
        private const val STATUS_ICON_UNIT_TEXT_FACTOR = 0.42f
        private const val STATUS_ICON_VALUE_TEXT_SCALE_X = 0.68f
        private const val STATUS_ICON_UNIT_TEXT_SCALE_X = 0.78f
        private const val STATUS_ICON_VALUE_CENTER_Y_FACTOR = 0.34f
        private const val STATUS_ICON_UNIT_CENTER_Y_FACTOR = 0.82f
        private const val STATUS_ICON_MIN_TEXT_SIZE = 8f
        private const val STATUS_ICON_TEXT_SHRINK_STEP = 1f

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

        private const val STYLE_DEFAULT = 1
    }
}
