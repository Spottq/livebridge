package com.nowbar.api.effects

import androidx.annotation.ColorInt
import androidx.annotation.FloatRange
import androidx.annotation.IntRange

/**
 * Animation timing and configuration extracted from Samsung sesl-visualeffect v2.0.36.
 *
 * Source: AnimationFactory$Companion$AnimationType enum with duration, interpolator, from/to values.
 * Duration hint values from b.java: NOW_BAR Phase1=4 frames, Phase2=44 frames, HIDE=36 frames.
 */

// ---------------------------------------------------------------------------
// Show / Hide animation type enums
// ---------------------------------------------------------------------------

/** Animation types for showing the GuidingLight effect */
enum class ShowAnimationType {
    /** No animation - instant show */
    NONE,
    /** Size-based show with 2-phase scale animation */
    SIZE,
    /** Luminance fade-in (fast, 200ms) */
    LUMINANCE,
    /** Luminance fade-in (slow, 1000ms) */
    LUMINANCE_LONG,
    /** Now Bar specific: 2-phase scale + luminance (Phase1=4 frames, Phase2=44 frames) */
    NOW_BAR,
    /** Now Bar shortcut: single phase scale + hide sequence */
    NOW_BAR_SHORTCUT
}

/** Animation types for hiding the GuidingLight effect */
enum class HideAnimationType {
    /** No animation - instant hide */
    NONE,
    /** Luminance fade-out (fast, 200ms) */
    LUMINANCE,
    /** Luminance fade-out (slow, 1000ms) */
    LUMINANCE_LONG
}

// ---------------------------------------------------------------------------
// Individual animation type configurations
// ---------------------------------------------------------------------------

/**
 * Configuration for a single animation phase.
 * Extracted from AnimationFactory$Companion$AnimationType enum values.
 *
 * @param durationMs Duration in milliseconds
 * @param fromValue Starting animated value
 * @param toValue Ending animated value
 * @param interpolatorControlPoints PathInterpolator control points (x1, y1, x2, y2)
 */
data class AnimationPhaseConfig(
    val durationMs: Long,
    val fromValue: Float,
    val toValue: Float,
    val interpolatorControlPoints: FloatArray = floatArrayOf(0.33f, 0.0f, 0.4f, 1.0f)
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AnimationPhaseConfig) return false
        return durationMs == other.durationMs &&
            fromValue == other.fromValue &&
            toValue == other.toValue &&
            interpolatorControlPoints.contentEquals(other.interpolatorControlPoints)
    }

    override fun hashCode(): Int {
        var result = durationMs.hashCode()
        result = 31 * result + fromValue.hashCode()
        result = 31 * result + toValue.hashCode()
        result = 31 * result + interpolatorControlPoints.contentHashCode()
        return result
    }
}

/**
 * All Samsung animation type definitions with exact values from decompiled code.
 */
object AnimationTypes {

    // -- SHOW: Size animations (standard) --

    /** Standard show phase 1: scale 0.0 -> 1.25 over 800ms */
    val SHOW_SIZE_PHASE_1 = AnimationPhaseConfig(
        durationMs = 800L,
        fromValue = 0.0f,
        toValue = 1.25f,
        interpolatorControlPoints = floatArrayOf(0.22f, 0.35f, 0.35f, 1.0f)
    )

    /** Standard show phase 2: scale 1.25 -> 1.0 over 850ms (overshoot settle) */
    val SHOW_SIZE_PHASE_2 = AnimationPhaseConfig(
        durationMs = 850L,
        fromValue = 1.25f,
        toValue = 1.0f,
        interpolatorControlPoints = floatArrayOf(0.33f, 0.0f, 0.4f, 1.0f)
    )

    // -- SHOW: Luminance animations --

    /** Fast luminance fade-in: 0.0 -> 1.0 over 200ms (linear) */
    val SHOW_LUMINANCE = AnimationPhaseConfig(
        durationMs = 200L,
        fromValue = 0.0f,
        toValue = 1.0f,
        interpolatorControlPoints = floatArrayOf(0.0f, 0.0f, 1.0f, 1.0f)
    )

    /** Slow luminance fade-in: 0.0 -> 1.0 over 1000ms */
    val SHOW_LUMINANCE_LONG = AnimationPhaseConfig(
        durationMs = 1000L,
        fromValue = 0.0f,
        toValue = 1.0f,
        interpolatorControlPoints = floatArrayOf(0.33f, 0.0f, 0.4f, 1.0f)
    )

    // -- HIDE: Luminance animations --

    /** Fast luminance fade-out: 1.0 -> 0.0 over 200ms (linear) */
    val HIDE_LUMINANCE = AnimationPhaseConfig(
        durationMs = 200L,
        fromValue = 1.0f,
        toValue = 0.0f,
        interpolatorControlPoints = floatArrayOf(0.0f, 0.0f, 1.0f, 1.0f)
    )

    /** Slow luminance fade-out: 1.0 -> 0.0 over 1000ms */
    val HIDE_LUMINANCE_LONG = AnimationPhaseConfig(
        durationMs = 1000L,
        fromValue = 1.0f,
        toValue = 0.0f,
        interpolatorControlPoints = floatArrayOf(0.33f, 0.0f, 0.4f, 1.0f)
    )

    // -- NOW BAR specific animations --

    /** Now Bar show phase 1: scale 0.0 -> 1.25 over 800ms (same curve as standard) */
    val SHOW_SIZE_NOW_BAR_PHASE_1 = AnimationPhaseConfig(
        durationMs = 800L,
        fromValue = 0.0f,
        toValue = 1.25f,
        interpolatorControlPoints = floatArrayOf(0.22f, 0.35f, 0.35f, 1.0f)
    )

    /** Now Bar show phase 2: scale 1.25 -> 1.0 over 850ms */
    val SHOW_SIZE_NOW_BAR_PHASE_2 = AnimationPhaseConfig(
        durationMs = 850L,
        fromValue = 1.25f,
        toValue = 1.0f,
        interpolatorControlPoints = floatArrayOf(0.33f, 0.0f, 0.4f, 1.0f)
    )

    /** Now Bar hide: scale 1.25 -> 0.45 over 850ms */
    val HIDE_SIZE_NOW_BAR = AnimationPhaseConfig(
        durationMs = 850L,
        fromValue = 1.25f,
        toValue = 0.45f,
        interpolatorControlPoints = floatArrayOf(0.33f, 0.0f, 0.4f, 1.0f)
    )
}

/**
 * Duration frame hints used in b.java's yu3.w0() calls for scheduling animation chains.
 * These are frame-count values passed as the last parameter to the animation factory.
 *
 * At 60fps: frames / 60 = seconds.
 *   4 frames  ~= 67ms
 *  36 frames  ~= 600ms
 *  44 frames  ~= 733ms
 *  52 frames  ~= 867ms
 */
object AnimationFrameHints {
    /** NOW_BAR Phase 1 start delay hint */
    const val NOW_BAR_PHASE_1 = 4
    /** NOW_BAR Phase 2 / luminance hint */
    const val NOW_BAR_PHASE_2 = 44
    /** NOW_BAR hide animation hint */
    const val NOW_BAR_HIDE = 36
    /** Standard SIZE Phase 1 hint */
    const val SIZE_PHASE_1 = 36
    /** Standard SIZE Phase 2 hint */
    const val SIZE_PHASE_2 = 44
    /** Hide luminance hint */
    const val HIDE_LUMINANCE = 52
    /** Show luminance hint */
    const val SHOW_LUMINANCE = 44
}

/**
 * Ripple effect animation configuration.
 * Extracted from RippleShader.java.
 */
data class RippleFadeParams(
    /** Start progress for fade-in. Default: 0.0 (immediate) */
    @FloatRange(from = 0.0, to = 1.0)
    val fadeInStart: Float = 0.0f,

    /** End progress for fade-in. Default: 0.15 */
    @FloatRange(from = 0.0, to = 1.0)
    val fadeInEnd: Float = 0.15f,

    /** Start progress for fade-out. Configurable per-ripple. */
    @FloatRange(from = 0.0, to = 1.0)
    val fadeOutStart: Float = 0.0f,

    /** End progress for fade-out. Default: 1.0 */
    @FloatRange(from = 0.0, to = 1.0)
    val fadeOutEnd: Float = 1.0f
)

/**
 * Ripple size-at-progress keyframe.
 */
data class RippleSizeAtProgress(
    /** Progress value (0..1) */
    @FloatRange(from = 0.0, to = 1.0)
    val t: Float = 0.0f,
    /** Width at this progress */
    val width: Float = 0.0f,
    /** Height at this progress */
    val height: Float = 0.0f
)

/**
 * Ripple shader configuration constants.
 */
object RippleConfig {
    /** PathInterpolator control points for ripple animation: (0.2, 0.0, 0.0, 1.0) */
    val INTERPOLATOR_CONTROL_POINTS = floatArrayOf(0.2f, 0.0f, 0.0f, 1.0f)
}

/**
 * Turbulence noise animation configuration.
 * All fields extracted from TurbulenceNoiseAnimationConfig.toString() output.
 */
data class TurbulenceNoiseConfig(
    val gridCount: Float = 0.0f,
    val luminosityMultiplier: Float = 0.0f,
    val noiseMoveSpeedX: Float = 0.0f,
    val noiseMoveSpeedY: Float = 0.0f,
    val noiseMoveSpeedZ: Float = 0.0f,
    @ColorInt val color: Int = 0,
    @ColorInt val backgroundColor: Int = 0,
    @IntRange(from = 0, to = 255) val opacity: Int = 0,
    val width: Float = 0.0f,
    val height: Float = 0.0f,
    val maxDuration: Float = 0.0f,
    val easeInDuration: Float = 0.0f,
    val easeOutDuration: Float = 0.0f,
    val pixelDensity: Float = 0.0f,
    val blendMode: TurbulenceBlendMode = TurbulenceBlendMode.SRC_OVER,
    val lumaMatteBlendFactor: Float = 0.0f,
    val lumaMatteOverallBrightness: Float = 0.0f
)

/** Blend modes for turbulence noise (default: SRC_OVER) */
enum class TurbulenceBlendMode {
    SRC_OVER,
    SRC_ATOP,
    MULTIPLY,
    SCREEN
}

/** Turbulence noise animation states */
enum class TurbulenceAnimationState {
    EASE_IN,
    MAIN,
    EASE_OUT,
    NOT_PLAYING
}