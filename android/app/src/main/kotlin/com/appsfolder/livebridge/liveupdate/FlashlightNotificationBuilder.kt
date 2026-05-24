package com.kakao.taxi.liveupdate

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.View
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.IconCompat
import com.kakao.taxi.MainActivity
import com.kakao.taxi.R

internal class FlashlightNotificationBuilder(
    private val context: Context
) {
    private data class RemoteViewTextColors(
        val primary: Int,
        val warning: Int
    )

    fun build(
        prefs: ConverterPrefs,
        capability: FlashlightCapability
    ): Notification {
        val sourceSnapshot = FlashlightSourceState.snapshot()
        val title = notificationTitle()
        val levelIndex = prefs.getSmartFlashlightLevel().coerceIn(0, FlashlightController.FLASHLIGHT_LEVEL_COUNT - 1)
        val effectiveLevelIndex = if (capability.supportsFiveLevels) {
            levelIndex
        } else {
            FlashlightController.DEFAULT_LEVEL_INDEX
        }
        val secondaryText = secondaryText(capability)
        val nowBarCollapsedText = chipText()
        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val expandedView = buildExpandedRemoteViews(
            title = title,
            capability = capability,
            effectiveLevelIndex = effectiveLevelIndex
        )
        val nowBarRemoteView = buildNowBarRemoteViews(
            title = title,
            capability = capability,
            effectiveLevelIndex = effectiveLevelIndex
        )
        val statusBarIconCompat = IconCompat.createWithResource(
            context,
            R.drawable.ic_flashlight_system_notification
        )
        val expandedLargeIcon = sourceSnapshot.largeIconBitmap ?: iconToBitmap(statusBarIconCompat)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_flashlight_system_notification)
            .setContentTitle(title)
            .setContentIntent(contentIntent)
            .setColor(sourceSnapshot.accentColor)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setCustomBigContentView(expandedView)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setRequestPromotedOngoing(true)
        expandedLargeIcon?.let(builder::setLargeIcon)

        if (!secondaryText.isNullOrEmpty()) {
            builder.setContentText(secondaryText)
        }
        if (nowBarCollapsedText.isNotEmpty()) {
            builder.setShortCriticalText(nowBarCollapsedText)
        }

        if (SamsungLiveUpdateReparser.isSamsungDevice()) {
            builder.addExtras(
                buildSamsungExtras(
                    title = title,
                    chipText = nowBarCollapsedText,
                    chipIcon = statusBarIconCompat,
                    remoteView = nowBarRemoteView,
                    chipBackgroundColor = sourceSnapshot.accentColor
                )
            )
        }

        val notification = builder.build()
        return if (SamsungLiveUpdateReparser.isSamsungDevice()) {
            SamsungOneUi7NowBarCompat.markEligible(notification)
        } else {
            notification
        }
    }

    private fun buildExpandedRemoteViews(
        title: String,
        capability: FlashlightCapability,
        effectiveLevelIndex: Int
    ): RemoteViews {
        return RemoteViews(context.packageName, R.layout.notification_flashlight_expanded).apply {
            setTextViewText(R.id.flashlight_title, title)
            setTextViewText(R.id.flashlight_action_button, disableButtonText())
            setOnClickPendingIntent(R.id.flashlight_action_button, disablePendingIntent())
            applyRemoteViewTheme(includeWarning = true)
            val warning = warningText(capability)
            if (warning.isNullOrEmpty()) {
                setViewVisibility(R.id.flashlight_warning, View.GONE)
            } else {
                setViewVisibility(R.id.flashlight_warning, View.VISIBLE)
                setTextViewText(R.id.flashlight_warning, warning)
            }
            applySliderState(
                remoteViews = this,
                capability = capability,
                effectiveLevelIndex = effectiveLevelIndex,
                interactive = capability.supportsFiveLevels
            )
        }
    }

    private fun buildNowBarRemoteViews(
        title: String,
        capability: FlashlightCapability,
        effectiveLevelIndex: Int
    ): RemoteViews {
        return RemoteViews(context.packageName, R.layout.notification_flashlight_slider).apply {
            setTextViewText(R.id.flashlight_title, title)
            setTextViewText(R.id.flashlight_action_button, disableButtonText())
            setOnClickPendingIntent(R.id.flashlight_action_button, disablePendingIntent())
            applyRemoteViewTheme(includeWarning = false)
            applySliderState(
                remoteViews = this,
                capability = capability,
                effectiveLevelIndex = effectiveLevelIndex,
                interactive = capability.supportsFiveLevels
            )
        }
    }

    private fun applySliderState(
        remoteViews: RemoteViews,
        capability: FlashlightCapability,
        effectiveLevelIndex: Int,
        interactive: Boolean
    ) {
        val segmentSlotIds = intArrayOf(
            R.id.flashlight_segment_slot_1,
            R.id.flashlight_segment_slot_2,
            R.id.flashlight_segment_slot_3,
            R.id.flashlight_segment_slot_4,
            R.id.flashlight_segment_slot_5
        )
        val segmentImageIds = intArrayOf(
            R.id.flashlight_segment_1,
            R.id.flashlight_segment_2,
            R.id.flashlight_segment_3,
            R.id.flashlight_segment_4,
            R.id.flashlight_segment_5
        )
        val selectedIndex = effectiveLevelIndex.coerceIn(0, segmentImageIds.lastIndex)
        for (index in segmentImageIds.indices) {
            val isSelected = interactive && index == selectedIndex
            remoteViews.setImageViewResource(
                segmentImageIds[index],
                if (isSelected) {
                    R.drawable.flashlight_segment_active
                } else {
                    R.drawable.flashlight_segment_inactive
                }
            )
            if (interactive) {
                val pendingIntent = levelPendingIntent(index)
                remoteViews.setOnClickPendingIntent(
                    segmentSlotIds[index],
                    pendingIntent
                )
                remoteViews.setOnClickPendingIntent(
                    segmentImageIds[index],
                    pendingIntent
                )
            }
        }
        val unsupported = capability.available && !capability.supportsFiveLevels
        remoteViews.setViewVisibility(
            R.id.flashlight_disabled_overlay,
            if (unsupported) View.VISIBLE else View.GONE
        )
    }

    private fun RemoteViews.applyRemoteViewTheme(includeWarning: Boolean) {
        val textColors = remoteViewTextColors()
        setInt(R.id.flashlight_notification_root, "setLayoutDirection", View.LAYOUT_DIRECTION_LTR)
        setTextColor(R.id.flashlight_title, textColors.primary)
        setTextColor(R.id.flashlight_action_button, textColors.primary)
        if (includeWarning) {
            setTextColor(R.id.flashlight_warning, textColors.warning)
        }
    }

    private fun remoteViewTextColors(): RemoteViewTextColors {
        val nightMode = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return if (nightMode == Configuration.UI_MODE_NIGHT_YES) {
            RemoteViewTextColors(
                primary = DARK_THEME_TEXT_COLOR,
                warning = DARK_THEME_WARNING_TEXT_COLOR
            )
        } else {
            RemoteViewTextColors(
                primary = LIGHT_THEME_TEXT_COLOR,
                warning = LIGHT_THEME_WARNING_TEXT_COLOR
            )
        }
    }

    private fun buildSamsungExtras(
        title: String,
        chipText: String?,
        chipIcon: IconCompat,
        remoteView: RemoteViews,
        chipBackgroundColor: Int
    ): Bundle {
        val icon = runCatching { chipIcon.toIcon(context) }.getOrNull()
        return Bundle().apply {
            putInt(KEY_STYLE, STYLE_DEFAULT)
            putCharSequence(KEY_PRIMARY_INFO, title)
            putCharSequence(KEY_CHIP_EXPANDED_TEXT, chipText)
            putCharSequence(KEY_NOWBAR_PRIMARY_INFO, title)
            putInt(KEY_CHIP_BG_COLOR, chipBackgroundColor)
            putBoolean(KEY_SHOW_SMALL_ICON, true)
            icon?.let {
                putParcelable(KEY_CHIP_ICON, it)
                putParcelable(KEY_NOWBAR_ICON, it)
                putParcelable(KEY_FIRST_ICON, it)
                putParcelable(KEY_SECONDARY_INFO_ICON, it)
            }
            putParcelable(KEY_REMOTE_VIEW, remoteView)
            putInt(KEY_REMOTE_VIEW_POSITION, 1)
            putString(KEY_REMOTE_VIEW_TAG, REMOTE_VIEW_TAG)
            putInt(KEY_NOWBAR_CHRONOMETER_POSITION, 1)
        }
    }

    private fun iconToBitmap(iconCompat: IconCompat): Bitmap? {
        return runCatching {
            iconCompat.toIcon(context).loadDrawable(context)?.let(::drawableToBitmap)
        }.getOrNull()
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        if (drawable is BitmapDrawable && drawable.bitmap != null) {
            return drawable.bitmap
        }
        val width = drawable.intrinsicWidth.coerceAtLeast(1).coerceAtMost(512)
        val height = drawable.intrinsicHeight.coerceAtLeast(1).coerceAtMost(512)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    private fun levelPendingIntent(levelIndex: Int): PendingIntent {
        return PendingIntent.getService(
            context,
            levelIndex + 1,
            Intent(context, FlashlightForegroundService::class.java).apply {
                action = FlashlightForegroundService.ACTION_SET_LEVEL
                putExtra(FlashlightForegroundService.EXTRA_LEVEL_INDEX, levelIndex)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun disablePendingIntent(): PendingIntent {
        return PendingIntent.getService(
            context,
            REQUEST_CODE_DISABLE,
            Intent(context, FlashlightForegroundService::class.java).apply {
                action = FlashlightForegroundService.ACTION_DISABLE
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun secondaryText(
        capability: FlashlightCapability
    ): String? {
        if (!capability.available) {
            return if (isRussianLocale()) {
                "\u0424\u043e\u043d\u0430\u0440\u0438\u043a \u043d\u0435\u0434\u043e\u0441\u0442\u0443\u043f\u0435\u043d"
            } else {
                "Flashlight unavailable"
            }
        }
        if (!capability.supportsFiveLevels) {
            return if (isRussianLocale()) {
                "\u042f\u0440\u043a\u043e\u0441\u0442\u044c 1/5 \u043d\u0435 \u043f\u043e\u0434\u0434\u0435\u0440\u0436\u0438\u0432\u0430\u0435\u0442\u0441\u044f"
            } else {
                "5-step brightness is unavailable"
            }
        }
        return null
    }

    private fun warningText(capability: FlashlightCapability): String? {
        if (!capability.available) {
            return if (isRussianLocale()) {
                "\u041d\u0430 \u044d\u0442\u043e\u043c \u0443\u0441\u0442\u0440\u043e\u0439\u0441\u0442\u0432\u0435 \u043d\u0435\u0442 \u0434\u043e\u0441\u0442\u0443\u043f\u043d\u043e\u0433\u043e \u0444\u043e\u043d\u0430\u0440\u0438\u043a\u0430."
            } else {
                "This device does not expose a usable flashlight."
            }
        }
        if (capability.supportsFiveLevels) {
            return null
        }
        return if (isRussianLocale()) {
            "\u0423\u0441\u0442\u0440\u043e\u0439\u0441\u0442\u0432\u043e \u043c\u043e\u0436\u0435\u0442 \u0442\u043e\u043b\u044c\u043a\u043e \u0432\u043a\u043b\u044e\u0447\u0430\u0442\u044c \u0444\u043e\u043d\u0430\u0440\u0438\u043a \u0431\u0435\u0437 5 \u0443\u0440\u043e\u0432\u043d\u0435\u0439 \u044f\u0440\u043a\u043e\u0441\u0442\u0438."
        } else {
            "This device can turn the flashlight on, but it does not expose 5 brightness levels."
        }
    }

    private fun notificationTitle(): String {
        return if (isRussianLocale()) {
            "\u0424\u043e\u043d\u0430\u0440\u0438\u043a \u0432\u043a\u043b\u044e\u0447\u0435\u043d"
        } else {
            "Flashlight on"
        }
    }

    private fun chipText(): String {
        return "Flashlight"
    }

    private fun disableButtonText(): String {
        return if (isRussianLocale()) {
            "\u041e\u0442\u043a\u043b\u044e\u0447\u0438\u0442\u044c"
        } else {
            "Turn off"
        }
    }

    private fun isRussianLocale(): Boolean {
        val locale = context.resources.configuration.locales.get(0)
        return locale?.language?.startsWith("ru", ignoreCase = true) == true
    }

    companion object {
        const val CHANNEL_ID = "livebridge_flashlight_nowbar"
        const val NOTIFICATION_ID = 41241

        private const val ONGOING_PREFIX = "android.ongoingActivityNoti."
        private const val KEY_STYLE = "${ONGOING_PREFIX}style"
        private const val KEY_PRIMARY_INFO = "${ONGOING_PREFIX}primaryInfo"
        private const val KEY_CHIP_BG_COLOR = "${ONGOING_PREFIX}chipBgColor"
        private const val KEY_CHIP_ICON = "${ONGOING_PREFIX}chipIcon"
        private const val KEY_CHIP_EXPANDED_TEXT = "${ONGOING_PREFIX}chipExpandedText"
        private const val KEY_FIRST_ICON = "${ONGOING_PREFIX}firstIcon"
        private const val KEY_NOWBAR_ICON = "${ONGOING_PREFIX}nowbarIcon"
        private const val KEY_NOWBAR_PRIMARY_INFO = "${ONGOING_PREFIX}nowbarPrimaryInfo"
        private const val KEY_SECONDARY_INFO_ICON = "${ONGOING_PREFIX}secondaryInfoIcon"
        private const val KEY_SHOW_SMALL_ICON = "android.showSmallIcon"
        private const val KEY_REMOTE_VIEW = "${ONGOING_PREFIX}chronometerRemoteView"
        private const val KEY_REMOTE_VIEW_POSITION = "${ONGOING_PREFIX}chronometerRemoteViewPosition"
        private const val KEY_REMOTE_VIEW_TAG = "${ONGOING_PREFIX}chronometerRemoteViewTag"
        private const val KEY_NOWBAR_CHRONOMETER_POSITION = "${ONGOING_PREFIX}nowbarChronometerPosition"
        private const val STYLE_DEFAULT = 1
        private const val DARK_THEME_TEXT_COLOR = 0xFFF5F7FA.toInt()
        private const val DARK_THEME_WARNING_TEXT_COLOR = 0xFF9CA3AF.toInt()
        private const val LIGHT_THEME_TEXT_COLOR = 0xFF111827.toInt()
        private const val LIGHT_THEME_WARNING_TEXT_COLOR = 0xFF4B5563.toInt()
        private const val REMOTE_VIEW_TAG = "flashlight_segments_remote"
        private const val REQUEST_CODE_DISABLE = 500
    }
}
