package com.nowbar.api.notification

import androidx.core.graphics.drawable.IconCompat

/**
 * Intermediate representation of ProgressStyle configuration produced by
 * [ProgressStyleAdapter] and consumed by [LiveUpdateBuilder].
 */
data class ProgressStyleConfig(
    val segments: List<StyleSegment> = emptyList(),
    val points: List<StylePoint> = emptyList(),
    val trackerIcon: IconCompat? = null,
    val startIcon: IconCompat? = null,
    val endIcon: IconCompat? = null
)

/** A colored segment in the progress bar with a relative length. */
data class StyleSegment(
    val length: Int,
    val color: Int,
    val id: Int = 0
)

/** A milestone point at a specific position on the progress bar. */
data class StylePoint(
    val position: Int,
    val color: Int,
    val id: Int = 0
)