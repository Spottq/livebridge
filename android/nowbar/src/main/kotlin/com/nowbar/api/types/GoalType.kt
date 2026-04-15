package com.nowbar.api.types

/**
 * Exercise goal types used by Samsung Health live tracker.
 * Values map directly to Samsung Health internal goal type IDs.
 *
 * Valid range per ExerciseLogger: 0..12 and 16.
 * "Light targets" (minimal overlay): PACE_COACHING, HEART_RATE_ZONE, VO2MAX, INTERVAL, RACE.
 */
enum class GoalType(val code: Int) {
    /** No goal — basic tracking without target. */
    NONE(0),

    /** Distance target in meters. */
    DISTANCE(1),

    /** Duration/time target in seconds. */
    DURATION(2),

    /** Calorie burn target. */
    CALORIES(3),

    /** Pace coaching / pace setter target. Light target. */
    PACE_COACHING(4),

    /** Heart rate zone target. Light target. */
    HEART_RATE_ZONE(8),

    /** Interval training goal. Light target. */
    INTERVAL(9),

    /** VO2 max training target. Light target. */
    VO2MAX(10),

    /** Race / virtual partner target. Light target. */
    RACE(11),

    /** Step count target. */
    STEP_COUNT(12),

    /** Sets and reps goal for countable exercises. */
    SETS_REPS(13),

    /** Custom pace target (used with cycling and running). */
    CUSTOM_PACE(16);

    /** Whether this goal shows a minimal "light" overlay in the Now Bar. */
    val isLightTarget: Boolean
        get() = this in LIGHT_TARGETS

    companion object {
        private val codeMap = entries.associateBy { it.code }
        private val LIGHT_TARGETS = setOf(PACE_COACHING, HEART_RATE_ZONE, VO2MAX, INTERVAL, RACE)

        fun fromCode(code: Int): GoalType? = codeMap[code]

        fun fromCodeOrNone(code: Int): GoalType = codeMap[code] ?: NONE

        /** Validate that a goal code is in the valid range (0..12 or 16). */
        fun isValid(code: Int): Boolean = (code in 0..12) || code == 16
    }
}