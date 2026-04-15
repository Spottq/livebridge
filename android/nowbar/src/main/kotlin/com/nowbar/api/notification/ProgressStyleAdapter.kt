package com.nowbar.api.notification

import android.graphics.Color
import com.nowbar.api.cards.CallCard
import com.nowbar.api.cards.CustomCard
import com.nowbar.api.cards.MediaCard
import com.nowbar.api.cards.NavigationCard
import com.nowbar.api.cards.NowBarCard
import com.nowbar.api.cards.TimerCard
import com.nowbar.api.cards.WorkoutCard

/**
 * Converts [NowBarCard] types into [ProgressStyleConfig] for Android 16 ProgressStyle.
 *
 * Each card type maps to a specific visual treatment:
 * - [TimerCard] — single progress bar, countdown chronometer chip
 * - [WorkoutCard] — colored segments for distance milestones, activity-specific tracker icon
 * - Other cards — basic progress with chip text from card data
 */
object ProgressStyleAdapter {

    /**
     * Produce a full [ProgressStyleConfig] for the given card.
     */
    fun adapt(card: NowBarCard): ProgressStyleConfig {
        return when (card) {
            is TimerCard -> adaptTimer(card)
            is WorkoutCard -> adaptWorkout(card)
            is NavigationCard -> adaptNavigation(card)
            is CustomCard -> adaptCustom(card)
            is CallCard -> adaptGeneric(card)
            is MediaCard -> adaptGeneric(card)
        }
    }

    /**
     * Returns short text for the chip pill on the ambient surface.
     * Returns null if a chronometer should be used instead.
     */
    fun getChipText(card: NowBarCard): String? {
        return when (card) {
            is TimerCard -> null // uses chronometer instead
            is WorkoutCard -> card.chipText ?: formatWorkoutChip(card)
            is NavigationCard -> card.chipText ?: card.distanceToTurn
            is CustomCard -> card.chipText
            is CallCard -> card.chipText ?: card.callerName
            is MediaCard -> card.chipText ?: card.title
        }
    }

    /**
     * Returns true if the card should use a system chronometer for the chip
     * instead of static text. Applicable to countdown timers.
     */
    fun shouldUseChronometer(card: NowBarCard): Boolean {
        return card is TimerCard && card.isCountDown
    }

    // --- Timer ---

    private fun adaptTimer(card: TimerCard): ProgressStyleConfig {
        // Single segment spanning the full bar, colored with accent or default blue
        val color = card.accentColor ?: Color.parseColor("#2196F3")
        return ProgressStyleConfig(
            segments = listOf(StyleSegment(length = 100, color = color))
        )
    }

    // --- Workout ---

    private fun adaptWorkout(card: WorkoutCard): ProgressStyleConfig {
        val color = card.accentColor ?: Color.parseColor("#0FCF6E")

        // Build distance milestone segments if distance is available
        val segments = if (card.distance != null && card.distance > 0) {
            buildDistanceSegments(card, color)
        } else {
            listOf(StyleSegment(length = 100, color = color))
        }

        // Milestone points at distance markers (every km/mi)
        val points = if (card.distance != null && card.distance > 0) {
            buildDistancePoints(card, color)
        } else {
            emptyList()
        }

        return ProgressStyleConfig(
            segments = segments,
            points = points,
            trackerIcon = card.icon // use the card's activity icon as tracker
        )
    }

    /**
     * Build colored segments representing distance progress.
     * Completed distance is shown in the accent color, remaining in gray.
     */
    private fun buildDistanceSegments(card: WorkoutCard, color: Int): List<StyleSegment> {
        val progress = card.toProgress().coerceIn(0, 100)
        if (progress <= 0) return listOf(StyleSegment(100, Color.GRAY))
        if (progress >= 100) return listOf(StyleSegment(100, color))

        return listOf(
            StyleSegment(length = progress, color = color),
            StyleSegment(length = 100 - progress, color = Color.parseColor("#E0E0E0"))
        )
    }

    /**
     * Build milestone points at each whole kilometer/mile.
     * Points are placed proportionally along the progress bar.
     */
    private fun buildDistancePoints(card: WorkoutCard, color: Int): List<StylePoint> {
        val distance = card.distance ?: return emptyList()
        if (distance <= 0) return emptyList()

        // Place a point at each whole unit up to current distance
        val wholeUnits = distance.toInt()
        if (wholeUnits <= 0) return emptyList()

        // Cap at 20 points to avoid visual clutter
        val maxPoints = wholeUnits.coerceAtMost(20)
        val ceilingDistance = distance.coerceAtLeast(1.0)

        return (1..maxPoints).map { km ->
            val position = ((km / ceilingDistance) * 100).toInt().coerceIn(0, 100)
            StylePoint(position = position, color = color)
        }
    }

    private fun formatWorkoutChip(card: WorkoutCard): String? {
        // Prefer distance for chip, fall back to elapsed time
        card.distance?.let { d ->
            return "%.1f %s".format(d, card.distanceUnit.symbol)
        }
        val totalSeconds = card.elapsed.inWholeSeconds
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return "%d:%02d".format(minutes, seconds)
    }

    // --- Navigation ---

    private fun adaptNavigation(card: NavigationCard): ProgressStyleConfig {
        val color = card.accentColor ?: Color.parseColor("#4285F4")
        return ProgressStyleConfig(
            segments = listOf(StyleSegment(length = 100, color = color)),
            trackerIcon = card.turnIcon // turn arrow as progress tracker
        )
    }

    // --- Custom ---

    private fun adaptCustom(card: CustomCard): ProgressStyleConfig {
        val color = card.customProgressColor ?: card.accentColor ?: Color.parseColor("#2196F3")
        return ProgressStyleConfig(
            segments = listOf(StyleSegment(length = 100, color = color))
        )
    }

    // --- Generic fallback ---

    private fun adaptGeneric(card: NowBarCard): ProgressStyleConfig {
        val color = card.accentColor ?: Color.parseColor("#2196F3")
        return ProgressStyleConfig(
            segments = listOf(StyleSegment(length = 100, color = color))
        )
    }
}