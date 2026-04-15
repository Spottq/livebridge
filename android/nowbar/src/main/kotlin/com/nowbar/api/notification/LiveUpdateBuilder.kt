package com.nowbar.api.notification

import android.os.Build
import androidx.core.app.NotificationCompat
import com.nowbar.api.cards.NowBarCard
import com.nowbar.api.cards.TimerCard

/**
 * Thin AndroidX wrapper for Android 16 Live Updates / promoted ongoing notifications.
 *
 * No reflection, no hidden magic:
 * - request promoted ongoing,
 * - attach ProgressStyle when progress exists,
 * - configure a chip via short text or chronometer.
 */
class LiveUpdateBuilder {

    companion object {
        @JvmStatic
        fun isSupported(): Boolean = Build.VERSION.SDK_INT >= 36
    }

    fun apply(
        builder: NotificationCompat.Builder,
        card: NowBarCard,
        requestPromotedOngoing: Boolean = true
    ) {
        if (!isSupported() || !requestPromotedOngoing) return

        builder.setRequestPromotedOngoing(true)

        card.toProgress()?.let { progress ->
            val config = ProgressStyleAdapter.adapt(card)

            val style = NotificationCompat.ProgressStyle()
                .setStyledByProgress(false)
                .setProgress(progress)

            if (config.segments.isNotEmpty()) {
                style.setProgressSegments(
                    config.segments.map { segment ->
                        NotificationCompat.ProgressStyle.Segment(segment.length)
                            .setColor(segment.color)
                            .setId(segment.id)
                    }
                )
            }

            if (config.points.isNotEmpty()) {
                style.setProgressPoints(
                    config.points.map { point ->
                        NotificationCompat.ProgressStyle.Point(point.position)
                            .setColor(point.color)
                            .setId(point.id)
                    }
                )
            }

            config.trackerIcon?.let(style::setProgressTrackerIcon)
            config.startIcon?.let(style::setProgressStartIcon)
            config.endIcon?.let(style::setProgressEndIcon)

            builder.setStyle(style)
        }

        configureChip(builder, card)
    }

    private fun configureChip(
        builder: NotificationCompat.Builder,
        card: NowBarCard
    ) {
        if (ProgressStyleAdapter.shouldUseChronometer(card)) {
            val timer = card as TimerCard
            val whenTime = System.currentTimeMillis() + timer.remainingDuration.inWholeMilliseconds

            builder
                .setWhen(whenTime)
                .setShowWhen(true)
                .setUsesChronometer(true)
                .setChronometerCountDown(true)
            return
        }

        ProgressStyleAdapter.getChipText(card)
            ?.takeIf { it.isNotBlank() }
            ?.let(builder::setShortCriticalText)
    }
}
