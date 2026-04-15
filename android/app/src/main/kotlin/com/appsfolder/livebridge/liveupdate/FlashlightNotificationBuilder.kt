package com.kakao.taxi.liveupdate

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
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
        val chipText = chipText()
        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val contentView = buildNotificationRemoteViews(
            title = title,
            capability = capability
        )
        val expandedView = buildExpandedRemoteViews(
            title = title,
            capability = capability
        )
        val nowBarRemoteView = buildNowBarRemoteViews(
            capability = capability,
            effectiveLevelIndex = effectiveLevelIndex,
            sourceSnapshot = sourceSnapshot
        )
        val iconCompat = sourceSnapshot.iconCompat
            ?: IconCompat.createWithResource(context, R.drawable.ic_flashlight_badge)
        val smallIconCompat = sourceSnapshot.iconCompat
            ?: IconCompat.createWithResource(context, R.drawable.ic_flashlight_stat)
        val notificationIcon = smallIconCompat.toIcon(context)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(smallIconCompat)
            .setLargeIcon(notificationIcon)
            .setContentTitle(title)
            .setContentText(secondaryText)
            .setContentIntent(contentIntent)
            .setColor(sourceSnapshot.accentColor)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setCustomContentView(contentView)
            .setCustomBigContentView(expandedView)
            .setCustomHeadsUpContentView(contentView)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setRequestPromotedOngoing(true)
            .setShortCriticalText(chipText)

        if (SamsungLiveUpdateReparser.isSamsungDevice()) {
            builder.addExtras(
                buildSamsungExtras(
                    title = title,
                    chipText = chipText,
                    iconCompat = iconCompat,
                    remoteView = nowBarRemoteView,
                    chipBackgroundColor = sourceSnapshot.accentColor
                )
            )
        }

        return builder.build()
    }

    private fun buildNotificationRemoteViews(
        title: String,
        capability: FlashlightCapability
    ): RemoteViews {
        return RemoteViews(context.packageName, R.layout.notification_flashlight_expanded).apply {
            setTextViewText(R.id.flashlight_title, title)
            setTextViewText(R.id.flashlight_status, secondaryText(capability))
            setTextViewText(R.id.flashlight_action_button, disableButtonText())
            setOnClickPendingIntent(R.id.flashlight_action_button, disablePendingIntent())
            val warning = warningText(capability)
            if (warning.isNullOrEmpty()) {
                setViewVisibility(R.id.flashlight_warning, View.GONE)
            } else {
                setViewVisibility(R.id.flashlight_warning, View.VISIBLE)
                setTextViewText(R.id.flashlight_warning, warning)
            }
        }
    }

    private fun buildExpandedRemoteViews(
        title: String,
        capability: FlashlightCapability
    ): RemoteViews {
        return buildNotificationRemoteViews(
            title = title,
            capability = capability
        )
    }

    private fun buildNowBarRemoteViews(
        capability: FlashlightCapability,
        effectiveLevelIndex: Int,
        sourceSnapshot: FlashlightSourceSnapshot
    ): RemoteViews {
        return RemoteViews(context.packageName, R.layout.notification_flashlight_slider).apply {
            applySourceIcon(this, sourceSnapshot)
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
                remoteViews.setOnClickPendingIntent(
                    segmentImageIds[index],
                    levelPendingIntent(index)
                )
            }
        }
        val unsupported = capability.available && !capability.supportsFiveLevels
        remoteViews.setViewVisibility(
            R.id.flashlight_disabled_overlay,
            if (unsupported) View.VISIBLE else View.GONE
        )
    }

    private fun buildSamsungExtras(
        title: String,
        chipText: String,
        iconCompat: IconCompat,
        remoteView: RemoteViews,
        chipBackgroundColor: Int
    ): Bundle {
        val icon = runCatching { iconCompat.toIcon(context) }.getOrNull()
        return Bundle().apply {
            putInt(KEY_STYLE, STYLE_DEFAULT)
            putCharSequence(KEY_PRIMARY_INFO, title)
            putCharSequence(KEY_CHIP_EXPANDED_TEXT, chipText)
            putCharSequence(KEY_NOWBAR_PRIMARY_INFO, title)
            putInt(KEY_CHIP_BG_COLOR, chipBackgroundColor)
            putBoolean(KEY_SHOW_SMALL_ICON, false)
            icon?.let {
                putParcelable(KEY_CHIP_ICON, it)
                putParcelable(KEY_NOWBAR_ICON, it)
            }
            putParcelable(KEY_REMOTE_VIEW, remoteView)
            putInt(KEY_REMOTE_VIEW_POSITION, 1)
            putString(KEY_REMOTE_VIEW_TAG, REMOTE_VIEW_TAG)
            putInt(KEY_NOWBAR_CHRONOMETER_POSITION, 1)
        }
    }

    private fun applySourceIcon(
        remoteViews: RemoteViews,
        sourceSnapshot: FlashlightSourceSnapshot
    ) {
        val icon = sourceSnapshot.iconCompat?.toIcon(context)
        if (icon != null) {
            remoteViews.setImageViewIcon(R.id.flashlight_slider_icon, icon)
        } else {
            remoteViews.setImageViewResource(R.id.flashlight_slider_icon, R.drawable.ic_flashlight_badge)
        }
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

    private fun secondaryText(capability: FlashlightCapability): String {
        if (!capability.available) {
            return if (isRussianLocale()) {
                "\u0424\u043e\u043d\u0430\u0440\u0438\u043a \u043d\u0435\u0434\u043e\u0441\u0442\u0443\u043f\u0435\u043d"
            } else {
                "Flashlight unavailable"
            }
        }
        return onText()
    }

    private fun chipText(): String {
        return if (isRussianLocale()) {
            "\u0424\u043e\u043d\u0430\u0440\u0438\u043a"
        } else {
            "Flashlight"
        }
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

    private fun disableButtonText(): String {
        return if (isRussianLocale()) {
            "\u041e\u0442\u043a\u043b\u044e\u0447\u0438\u0442\u044c"
        } else {
            "Turn off"
        }
    }

    private fun onText(): String {
        return if (isRussianLocale()) {
            "\u0412\u043a\u043b\u044e\u0447\u0435\u043d\u043e"
        } else {
            "On"
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
        private const val KEY_NOWBAR_ICON = "${ONGOING_PREFIX}nowbarIcon"
        private const val KEY_NOWBAR_PRIMARY_INFO = "${ONGOING_PREFIX}nowbarPrimaryInfo"
        private const val KEY_SHOW_SMALL_ICON = "android.showSmallIcon"
        private const val KEY_REMOTE_VIEW = "${ONGOING_PREFIX}chronometerRemoteView"
        private const val KEY_REMOTE_VIEW_POSITION = "${ONGOING_PREFIX}chronometerRemoteViewPosition"
        private const val KEY_REMOTE_VIEW_TAG = "${ONGOING_PREFIX}chronometerRemoteViewTag"
        private const val KEY_NOWBAR_CHRONOMETER_POSITION = "${ONGOING_PREFIX}nowbarChronometerPosition"
        private const val STYLE_DEFAULT = 1
        private const val REMOTE_VIEW_TAG = "flashlight_segments_remote"
        private const val REQUEST_CODE_DISABLE = 500
    }
}
