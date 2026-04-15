package com.nowbar.api.cards

import android.app.PendingIntent
import androidx.core.graphics.drawable.IconCompat
import kotlin.time.Duration

data class CallCard(
    override val title: String,
    override val icon: IconCompat,
    override val accentColor: Int? = null,
    override val tapAction: PendingIntent? = null,
    override val chipText: String? = null,
    val callerName: String,
    val callerNumber: String? = null,
    val isIncoming: Boolean = true,
    val callDuration: Duration? = null,
    val answerAction: PendingIntent? = null,
    val declineAction: PendingIntent? = null,
    val hangupAction: PendingIntent? = null,
    val secondaryInfoIcon: IconCompat? = null
) : NowBarCard(
    type = CardType.CALL,
    title = title,
    icon = icon,
    accentColor = accentColor,
    tapAction = tapAction,
    chipText = chipText
) {
    override fun toPrimaryInfo(): String = callerName

    override fun toSecondaryInfo(): String {
        callDuration?.let { duration ->
            val totalSeconds = duration.inWholeSeconds
            val hours = totalSeconds / 3600
            val minutes = (totalSeconds % 3600) / 60
            val seconds = totalSeconds % 60

            return if (hours > 0) {
                "%d:%02d:%02d".format(hours, minutes, seconds)
            } else {
                "%02d:%02d".format(minutes, seconds)
            }
        }

        return if (isIncoming) "Incoming call" else "Outgoing call"
    }

    override fun toNowBarSecondaryInfo(): String = toSecondaryInfo()

    override fun toNowBarPrimaryInfo(): String = callerName

    override fun toSubstName(): String = title

    override fun toSecondaryInfoIcon(): IconCompat? = secondaryInfoIcon

    class Builder(
        private val title: String,
        private val icon: IconCompat,
        private val callerName: String
    ) {
        private var accentColor: Int? = null
        private var tapAction: PendingIntent? = null
        private var chipText: String? = null
        private var callerNumber: String? = null
        private var isIncoming: Boolean = true
        private var callDuration: Duration? = null
        private var answerAction: PendingIntent? = null
        private var declineAction: PendingIntent? = null
        private var hangupAction: PendingIntent? = null
        private var secondaryInfoIcon: IconCompat? = null

        fun accentColor(color: Int) = apply { this.accentColor = color }
        fun tapAction(action: PendingIntent) = apply { this.tapAction = action }
        fun chipText(text: String) = apply { this.chipText = text }
        fun callerNumber(number: String) = apply { this.callerNumber = number }
        fun isIncoming(incoming: Boolean) = apply { this.isIncoming = incoming }
        fun callDuration(duration: Duration) = apply { this.callDuration = duration }
        fun answerAction(action: PendingIntent) = apply { this.answerAction = action }
        fun declineAction(action: PendingIntent) = apply { this.declineAction = action }
        fun hangupAction(action: PendingIntent) = apply { this.hangupAction = action }
        fun secondaryInfoIcon(icon: IconCompat) = apply { this.secondaryInfoIcon = icon }

        fun build(): CallCard = CallCard(
            title = title,
            icon = icon,
            accentColor = accentColor,
            tapAction = tapAction,
            chipText = chipText,
            callerName = callerName,
            callerNumber = callerNumber,
            isIncoming = isIncoming,
            callDuration = callDuration,
            answerAction = answerAction,
            declineAction = declineAction,
            hangupAction = hangupAction,
            secondaryInfoIcon = secondaryInfoIcon
        )

        companion object {
            @JvmStatic
            fun create(title: String, icon: IconCompat, callerName: String) =
                Builder(title, icon, callerName)
        }
    }
}
