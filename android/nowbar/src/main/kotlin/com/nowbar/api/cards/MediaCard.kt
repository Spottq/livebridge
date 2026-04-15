package com.nowbar.api.cards

import android.app.PendingIntent
import android.graphics.Bitmap
import androidx.core.graphics.drawable.IconCompat

data class MediaCard(
    override val title: String,
    override val icon: IconCompat,
    override val accentColor: Int? = null,
    override val tapAction: PendingIntent? = null,
    override val chipText: String? = null,
    val artist: String? = null,
    val album: String? = null,
    val albumArt: Bitmap? = null,
    val isPlaying: Boolean = false,
    val playAction: PendingIntent? = null,
    val skipAction: PendingIntent? = null,
    val firstIcon: IconCompat? = null
) : NowBarCard(
    type = CardType.MEDIA,
    title = title,
    icon = icon,
    accentColor = accentColor,
    tapAction = tapAction,
    chipText = chipText
) {
    override fun toPrimaryInfo(): String = title

    override fun toSecondaryInfo(): String = artist ?: ""

    override fun toNowBarSecondaryInfo(): String? {
        return artist?.let { a ->
            album?.let { al -> "$a — $al" } ?: a
        }
    }

    override fun toNowBarPrimaryInfo(): String = title

    override fun toSubstName(): String = title

    override fun toFirstIcon(): IconCompat? = firstIcon

    class Builder(
        private val title: String,
        private val icon: IconCompat
    ) {
        private var accentColor: Int? = null
        private var tapAction: PendingIntent? = null
        private var chipText: String? = null
        private var artist: String? = null
        private var album: String? = null
        private var albumArt: Bitmap? = null
        private var isPlaying: Boolean = false
        private var playAction: PendingIntent? = null
        private var skipAction: PendingIntent? = null
        private var firstIcon: IconCompat? = null

        fun accentColor(color: Int) = apply { this.accentColor = color }
        fun tapAction(action: PendingIntent) = apply { this.tapAction = action }
        fun chipText(text: String) = apply { this.chipText = text }
        fun artist(artist: String) = apply { this.artist = artist }
        fun album(album: String) = apply { this.album = album }
        fun albumArt(bitmap: Bitmap) = apply { this.albumArt = bitmap }
        fun isPlaying(playing: Boolean) = apply { this.isPlaying = playing }
        fun playAction(action: PendingIntent) = apply { this.playAction = action }
        fun skipAction(action: PendingIntent) = apply { this.skipAction = action }
        fun firstIcon(icon: IconCompat) = apply { this.firstIcon = icon }

        fun build(): MediaCard = MediaCard(
            title = title,
            icon = icon,
            accentColor = accentColor,
            tapAction = tapAction,
            chipText = chipText,
            artist = artist,
            album = album,
            albumArt = albumArt,
            isPlaying = isPlaying,
            playAction = playAction,
            skipAction = skipAction,
            firstIcon = firstIcon
        )

        companion object {
            @JvmStatic
            fun create(title: String, icon: IconCompat) = Builder(title, icon)
        }
    }
}