package com.nowbar.api.effects

import android.graphics.PointF
import androidx.annotation.ColorInt
import androidx.annotation.FloatRange

/**
 * Complete visual configuration for Samsung Now Bar effects.
 * Aggregates all configurable parameters from sesl-visualeffect v2.0.36.
 *
 * This is the top-level config class that combines:
 * - GuidingLight shader parameters
 * - Animation timing configurations
 * - Ripple effect settings
 * - Turbulence noise settings
 * - EffectView frame rate and state management
 */
data class NowBarVisualConfig(
    /** GuidingLight shader and effect parameters */
    val guidingLight: GuidingLightParams = GuidingLightParams(),

    /** Show animation type. Default: NOW_BAR (2-phase scale with luminance) */
    val showAnimation: ShowAnimationType = ShowAnimationType.NOW_BAR,

    /** Hide animation type. Default: LUMINANCE (fast 200ms fade-out) */
    val hideAnimation: HideAnimationType = HideAnimationType.LUMINANCE,

    /** Frame rate category for the EffectView */
    val frameRateCategory: FrameRateCategory = FrameRateCategory.NORMAL,

    /** Progress bar color (light theme) */
    @ColorInt
    val progressColor: Int = NowBarColors.PROGRESS_LIGHT,

    /** Progress bar color (dark theme) */
    @ColorInt
    val progressColorDark: Int = NowBarColors.PROGRESS_DARK,

    /** Progress track color (light theme) */
    @ColorInt
    val progressTrackColor: Int = NowBarColors.PROGRESS_TRACK_LIGHT,

    /** Progress track color (dark theme) */
    @ColorInt
    val progressTrackColorDark: Int = NowBarColors.PROGRESS_TRACK_DARK,

    /** Whether to respect system accessibility: "remove_animations" setting */
    val respectReduceAnimations: Boolean = true,

    /** Whether to respect system "animator_duration_scale" setting */
    val respectAnimatorDurationScale: Boolean = true
) {
    companion object {
        /**
         * Samsung default configuration for sport tracker Now Bar.
         * Uses NOW_BAR show animation with 2-phase scale + luminance.
         */
        val SPORT_TRACKER = NowBarVisualConfig(
            guidingLight = GuidingLightParams(
                shape = LightShape.ROUND_RECT,
                roundRectDirection = RoundRectDirection.ALL,
                lightPos = PointF(0.5f, 0.5f),
                lightRadius = 1.92f,
                lightIntensity = 0.28f,
                glowIntensity = 0.28f,
                glowRadius = 1.25f,
                glowSharpness = 36.0f,
                reflLightIntensity = 0.48f,
                reflLightRadius = 1.82f,
                globalLuminance = 1.0f,
                ditherVariation = 0.07f,
                saturation = 1.15f,
                outerSaturation = 0.9f,
                stretch = 1.65f,
                lightMovement = LightMovement.DEFAULT,
                frameRate = 60.0f,
                outlineThickness = 48.0f
            ),
            showAnimation = ShowAnimationType.NOW_BAR,
            hideAnimation = HideAnimationType.LUMINANCE
        )

        /**
         * Minimal configuration - no light movement, instant show/hide.
         */
        val MINIMAL = NowBarVisualConfig(
            guidingLight = GuidingLightParams(
                lightMovement = LightMovement.NONE,
                saturation = 1.15f
            ),
            showAnimation = ShowAnimationType.NONE,
            hideAnimation = HideAnimationType.NONE
        )

        /**
         * Enhanced glow configuration with higher saturation.
         */
        val ENHANCED_GLOW = NowBarVisualConfig(
            guidingLight = GuidingLightParams(
                saturation = 1.25f,
                outerSaturation = 1.0f,
                glowIntensity = 0.35f,
                glowRadius = 1.5f
            ),
            showAnimation = ShowAnimationType.NOW_BAR,
            hideAnimation = HideAnimationType.LUMINANCE
        )
    }
}

/**
 * EffectView frame rate categories.
 * Extracted from EffectView.FrameRateCategory enum.
 *
 * Maps to requestedEffectFrameRate:
 *   LOW    -> 30.0 fps
 *   NORMAL -> 60.0 fps
 *   HIGH   -> null (unlimited / device max)
 */
enum class FrameRateCategory(val fps: Float?) {
    /** 30 fps - battery efficient */
    LOW(30.0f),
    /** 60 fps - standard smooth rendering */
    NORMAL(60.0f),
    /** Unlimited - uses device maximum frame rate. On API 35+, sets requestedFrameRate to -3.0 (FRAME_RATE_CATEGORY_NO_PREFERENCE) */
    HIGH(null)
}

/**
 * EffectView animation state.
 * Extracted from EffectView.AnimationState enum.
 */
enum class EffectAnimationState {
    /** Effect is initialized but not animating */
    READY,
    /** Effect animation is actively running */
    RUNNING
}

/**
 * Accessibility settings that Samsung checks before starting animations.
 * From b.java method d():
 *   - Settings.System "remove_animations" == 1 -> blocked
 *   - Settings.System "animator_duration_scale" == 0.0 -> blocked
 */
object AccessibilityChecks {
    const val SETTING_REMOVE_ANIMATIONS = "remove_animations"
    const val SETTING_ANIMATOR_DURATION_SCALE = "animator_duration_scale"
    const val BLOCKED_REMOVE_ANIMATIONS_VALUE = 1
    const val BLOCKED_DURATION_SCALE_VALUE = 0.0f
}