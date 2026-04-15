package com.nowbar.api.fallback

import android.app.Notification
import android.content.Context
import androidx.core.app.NotificationCompat
import com.nowbar.api.cards.CallCard
import com.nowbar.api.cards.MediaCard
import com.nowbar.api.cards.NavigationCard
import com.nowbar.api.cards.NowBarCard
import com.nowbar.api.cards.TimerCard
import com.nowbar.api.cards.WorkoutCard

object StandardNotificationAdapter {

    fun build(context: Context, channelId: String, card: NowBarCard): Notification {
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(card.icon)
            .setContentTitle(card.toPrimaryInfo())
            .setContentText(card.toSecondaryInfo())
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(getCategoryForCard(card))

        card.accentColor?.let(builder::setColor)
        card.tapAction?.let(builder::setContentIntent)

        card.toNowBarPrimaryInfo()
            ?.takeIf { it.isNotBlank() && it != card.toPrimaryInfo() }
            ?.let(builder::setSubText)

        card.toProgress()?.let { progress ->
            builder.setProgress(card.toProgressMax(), progress, false)
        }

        when (card) {
            is MediaCard -> {
                card.albumArt?.let(builder::setLargeIcon)

                card.playAction?.let { action ->
                    builder.addAction(0, if (card.isPlaying) "Pause" else "Play", action)
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

        return builder.build()
    }

    private fun getCategoryForCard(card: NowBarCard): String = when (card) {
        is TimerCard -> NotificationCompat.CATEGORY_STOPWATCH
        is WorkoutCard -> NotificationCompat.CATEGORY_WORKOUT
        is MediaCard -> NotificationCompat.CATEGORY_TRANSPORT
        is NavigationCard -> NotificationCompat.CATEGORY_NAVIGATION
        is CallCard -> NotificationCompat.CATEGORY_CALL
        else -> NotificationCompat.CATEGORY_PROGRESS
    }
}
