package com.nowbar.api.notification

import android.app.Notification
import android.content.Context
import android.graphics.drawable.Icon
import androidx.core.app.NotificationCompat
import com.nowbar.api.FeatureDetector
import com.nowbar.api.NowBarConfig
import com.nowbar.api.cards.CallCard
import com.nowbar.api.cards.CustomCard
import com.nowbar.api.cards.MediaCard
import com.nowbar.api.cards.NavigationCard
import com.nowbar.api.cards.NowBarCard
import com.nowbar.api.cards.TimerCard
import com.nowbar.api.cards.WorkoutCard
import com.nowbar.api.fallback.FallbackStrategyResolver
import com.nowbar.api.fallback.StandardNotificationAdapter

/**
 * One builder, one result.
 *
 * Base notification is always standard Android.
 * Then we layer optional capabilities on top:
 * - Samsung extras when the device supports them
 * - promoted ongoing / ProgressStyle on Android 16+
 */
class NowBarNotificationBuilder(
    private val context: Context,
    private val config: NowBarConfig
) {

    private val liveUpdateBuilder = LiveUpdateBuilder()

    fun build(card: NowBarCard): Notification =
        build(card, config.samsungStyle, config.requestPromotedOngoing)

    internal fun build(
        card: NowBarCard,
        samsungStyle: Int,
        requestPromotedOngoing: Boolean
    ): Notification {
        val fallback = FallbackStrategyResolver.resolve(
            strategy = config.fallbackStrategy,
            nativeSurfaceSupported = FeatureDetector.isNativeSurfaceSupported(context)
        )

        if (fallback.useStandardNotificationOnly) {
            return StandardNotificationAdapter.build(context, config.channelId, card)
        }

        val builder = NotificationCompat.Builder(context, config.channelId)
            .setSmallIcon(card.icon)
            .setContentTitle(card.toPrimaryInfo())
            .setContentText(card.toSecondaryInfo())
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(categoryFor(card))

        card.tapAction?.let(builder::setContentIntent)
        card.accentColor?.let(builder::setColor)

        card.toProgress()?.let { progress ->
            builder.setProgress(card.toProgressMax(), progress, false)
        }

        applyCardDetails(builder, card)

        liveUpdateBuilder.apply(
            builder = builder,
            card = card,
            requestPromotedOngoing = requestPromotedOngoing
        )

        if (FeatureDetector.isSamsungNowBarSupported(context)) {
            builder.addExtras(buildSamsungExtras(card, samsungStyle))
        }

        // Set substName and showSmallIcon directly on notification extras
        // (Samsung Clock sets these on the builder extras separately from the ongoing bundle)
        card.toSubstName()?.let { name ->
            builder.addExtras(android.os.Bundle().apply {
                putCharSequence("android.substName", name)
            })
        }
        builder.addExtras(android.os.Bundle().apply {
            putBoolean("android.showSmallIcon", config.showSmallIcon)
        })

        return builder.build()
    }

    private fun buildSamsungExtras(
        card: NowBarCard,
        samsungStyle: Int
    ) = OngoingExtrasBuilder()
        .setStyle(samsungStyle)
        .setActionType(OngoingExtrasBuilder.ACTION_TYPE_BUTTON_TEXT)
        .setShowSmallIcon(config.showSmallIcon)
        .setChipConfig(
            ChipConfig(
                icon = safeIcon(card),
                backgroundColor = card.toChipBackgroundColor(),
                expandedText = card.toChipText()
            )
        )
        .setPrimaryInfo(card.toPrimaryInfo())
        // Action primary set for collapsed Now Bar buttons
        .setActionPrimarySet(card.toActionPrimarySet())
        .apply {
            val secondary = card.toSecondaryInfo()
            if (secondary.isNotBlank()) setSecondaryInfo(secondary)

            card.toNowBarSecondaryInfo()
                ?.takeIf { it.isNotBlank() }
                ?.let(::setNowBarSecondaryInfo)

            card.toProgress()?.let { setProgress(it, card.toProgressMax()) }

            samsungProgressColor(card)?.let(::setProgressColor)

            samsungSegments(card)
                .takeIf { it.isNotEmpty() }
                ?.let(::setProgressSegments)

            card.toNowBarPrimaryInfo()
                ?.takeIf { it.isNotBlank() }
                ?.let(::setNowBarPrimaryInfo)

            safeNowBarIcon(card)?.let(::setNowBarIcon)
            safeSecondIcon(card)?.let(::setSecondIcon)
            safeFirstIcon(card)?.let(::setFirstIcon)
            safeSecondaryInfoIcon(card)?.let(::setSecondaryInfoIcon)
            card.toActionBgColor()?.let(::setActionBgColor)
            setActionPrimarySet(config.actionPrimarySet)

            card.toSubstName()?.takeIf { it.isNotBlank() }?.let(::setSubstName)
            card.toNowBarSubScreenIntent()?.let(::setNowBarSubScreenIntent)
        }
        .build()

    private fun safeIcon(card: NowBarCard): Icon? {
        return runCatching { card.toChipIcon().toIcon(context) }.getOrNull()
    }

    private fun safeNowBarIcon(card: NowBarCard): Icon? {
        return card.toNowBarIcon()?.let { icon ->
            runCatching { icon.toIcon(context) }.getOrNull()
        }
    }

    private fun safeSecondIcon(card: NowBarCard): Icon? {
        return card.toSecondIcon()?.let { icon ->
            runCatching { icon.toIcon(context) }.getOrNull()
        }
    }

    private fun safeFirstIcon(card: NowBarCard): Icon? {
        return card.toFirstIcon()?.let { icon ->
            runCatching { icon.toIcon(context) }.getOrNull()
        }
    }

    private fun safeSecondaryInfoIcon(card: NowBarCard): Icon? {
        return card.toSecondaryInfoIcon()?.let { icon ->
            runCatching { icon.toIcon(context) }.getOrNull()
        }
    }

    private fun samsungProgressColor(card: NowBarCard): Int? = when (card) {
        is CustomCard -> card.customProgressColor ?: card.accentColor
        else -> card.accentColor
    }

    /**
     * Keep Samsung Health-like segmented workout bars by default.
     * Real custom segment APIs are still preserved in OngoingExtrasBuilder for power users.
     */
    private fun samsungSegments(card: NowBarCard): List<ProgressSegment> {
        val color = samsungProgressColor(card) ?: return emptyList()

        return when (card) {
            is WorkoutCard -> listOf(
                ProgressSegment(startPosition = 0.0f, color = color),
                ProgressSegment(startPosition = 0.34f, color = color),
                ProgressSegment(startPosition = 0.67f, color = color)
            )
            else -> emptyList()
        }
    }

    private fun applyCardDetails(
        builder: NotificationCompat.Builder,
        card: NowBarCard
    ) {
        when (card) {
            is MediaCard -> {
                card.albumArt?.let(builder::setLargeIcon)

                card.playAction?.let { action ->
                    builder.addAction(
                        0,
                        if (card.isPlaying) "Pause" else "Play",
                        action
                    )
                }

                card.skipAction?.let { action ->
                    builder.addAction(0, "Next", action)
                }
            }

            is CallCard -> {
                if (card.isIncoming) {
                    card.answerAction?.let { builder.addAction(0, "Answer", it) }
                    card.declineAction?.let { builder.addAction(0, "Decline", it) }
                } else {
                    card.hangupAction?.let { builder.addAction(0, "Hang up", it) }
                }
            }

            else -> Unit
        }
    }

    private fun categoryFor(card: NowBarCard): String = when (card) {
        is TimerCard -> NotificationCompat.CATEGORY_STOPWATCH
        is WorkoutCard -> NotificationCompat.CATEGORY_WORKOUT
        is MediaCard -> NotificationCompat.CATEGORY_TRANSPORT
        is NavigationCard -> NotificationCompat.CATEGORY_NAVIGATION
        is CallCard -> NotificationCompat.CATEGORY_CALL
        else -> NotificationCompat.CATEGORY_PROGRESS
    }
}
