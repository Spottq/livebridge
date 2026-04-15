package com.nowbar.api.cards

import android.app.PendingIntent
import androidx.core.graphics.drawable.IconCompat

data class NavigationCard(
    override val title: String,
    override val icon: IconCompat,
    override val accentColor: Int? = null,
    override val tapAction: PendingIntent? = null,
    override val chipText: String? = null,
    val nextDirection: String,
    val distanceToTurn: String,
    val eta: String? = null,
    val turnIcon: IconCompat? = null
) : NowBarCard(
    type = CardType.NAVIGATION,
    title = title,
    icon = icon,
    accentColor = accentColor,
    tapAction = tapAction,
    chipText = chipText
) {
    override fun toPrimaryInfo(): String = nextDirection

    override fun toSecondaryInfo(): String {
        return eta?.let { "$distanceToTurn • ETA $it" } ?: distanceToTurn
    }

    override fun toNowBarSecondaryInfo(): String = distanceToTurn

    override fun toNowBarIcon(): IconCompat? = turnIcon

    override fun toSecondIcon(): IconCompat? = turnIcon

    override fun toNowBarPrimaryInfo(): String = distanceToTurn

    override fun toSubstName(): String = title

    class Builder(
        private val title: String,
        private val icon: IconCompat,
        private val nextDirection: String,
        private val distanceToTurn: String
    ) {
        private var accentColor: Int? = null
        private var tapAction: PendingIntent? = null
        private var chipText: String? = null
        private var eta: String? = null
        private var turnIcon: IconCompat? = null

        fun accentColor(color: Int) = apply { this.accentColor = color }
        fun tapAction(action: PendingIntent) = apply { this.tapAction = action }
        fun chipText(text: String) = apply { this.chipText = text }
        fun eta(eta: String) = apply { this.eta = eta }
        fun turnIcon(icon: IconCompat) = apply { this.turnIcon = icon }

        fun build(): NavigationCard = NavigationCard(
            title = title,
            icon = icon,
            accentColor = accentColor,
            tapAction = tapAction,
            chipText = chipText,
            nextDirection = nextDirection,
            distanceToTurn = distanceToTurn,
            eta = eta,
            turnIcon = turnIcon
        )

        companion object {
            @JvmStatic
            fun create(
                title: String,
                icon: IconCompat,
                nextDirection: String,
                distanceToTurn: String
            ) = Builder(title, icon, nextDirection, distanceToTurn)
        }
    }
}