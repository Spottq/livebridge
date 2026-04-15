package com.nowbar.api.cards

import android.app.PendingIntent
import androidx.core.graphics.drawable.IconCompat
import kotlin.time.Duration

data class TimerCard(
    override val title: String,
    override val icon: IconCompat,
    override val accentColor: Int? = null,
    override val tapAction: PendingIntent? = null,
    override val chipText: String? = null,
    val totalDuration: Duration,
    val remainingDuration: Duration,
    val isCountDown: Boolean = true,
    val completionAction: PendingIntent? = null,
    val subScreenIntent: PendingIntent? = null
) : NowBarCard(
    type = CardType.TIMER,
    title = title,
    icon = icon,
    accentColor = accentColor,
    tapAction = tapAction,
    chipText = chipText
) {
    override fun toPrimaryInfo(): String = title

    override fun toSecondaryInfo(): String {
        val totalSeconds = remainingDuration.inWholeSeconds
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }

    override fun toNowBarSecondaryInfo(): String = toSecondaryInfo()

    override fun toProgress(): Int {
        if (totalDuration.inWholeMilliseconds == 0L) return 0
        val elapsed = totalDuration - remainingDuration
        return ((elapsed.inWholeMilliseconds * 100) / totalDuration.inWholeMilliseconds).toInt()
            .coerceIn(0, 100)
    }

    override fun toProgressMax(): Int = 100

    override fun toNowBarPrimaryInfo(): String = toSecondaryInfo()

    override fun toSubstName(): String = title

    override fun hasChronometerSupport(): Boolean = true

    override fun toNowBarSubScreenIntent(): PendingIntent? = subScreenIntent

    class Builder(
        private val title: String,
        private val icon: IconCompat,
        private val totalDuration: Duration
    ) {
        private var accentColor: Int? = null
        private var tapAction: PendingIntent? = null
        private var chipText: String? = null
        private var remainingDuration: Duration = totalDuration
        private var isCountDown: Boolean = true
        private var completionAction: PendingIntent? = null
        private var subScreenIntent: PendingIntent? = null

        fun accentColor(color: Int) = apply { this.accentColor = color }
        fun tapAction(action: PendingIntent) = apply { this.tapAction = action }
        fun chipText(text: String) = apply { this.chipText = text }
        fun remainingDuration(duration: Duration) = apply { this.remainingDuration = duration }
        fun isCountDown(countDown: Boolean) = apply { this.isCountDown = countDown }
        fun completionAction(action: PendingIntent) = apply { this.completionAction = action }
        fun subScreenIntent(intent: PendingIntent) = apply { this.subScreenIntent = intent }

        fun build(): TimerCard = TimerCard(
            title = title,
            icon = icon,
            accentColor = accentColor,
            tapAction = tapAction,
            chipText = chipText,
            totalDuration = totalDuration,
            remainingDuration = remainingDuration,
            isCountDown = isCountDown,
            completionAction = completionAction,
            subScreenIntent = subScreenIntent
        )

        companion object {
            @JvmStatic
            fun create(title: String, icon: IconCompat, totalDuration: Duration) =
                Builder(title, icon, totalDuration)
        }
    }
}