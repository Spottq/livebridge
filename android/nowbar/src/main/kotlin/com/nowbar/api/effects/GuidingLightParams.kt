package com.nowbar.api.effects

import android.graphics.PointF
import androidx.annotation.ColorInt
import androidx.annotation.FloatRange

/**
 * Shader uniform parameters extracted from Samsung sesl-visualeffect library v2.0.36.
 * Maps shader uniform IDs to semantic parameter names.
 *
 * Uniform ID mapping (from GuidingLightEffect internal shader):
 *   0  = color (light color, set via kab/Color uniform)
 *   1  = refIntensity (reflection light intensity)
 *   2  = stretch
 *   4  = stretchDir (stretch direction for lit areas)
 *   6  = refAlbedo (reflection albedo)
 *   7  = glowIntensity (intensity of the glow effect)
 *   8  = glowSharpness
 *   9  = glowRadius
 *  10  = lightRadius (radius of the primary light)
 *  11  = outerSaturation
 *  12  = cornerRadiusPixel
 *  13  = extra (outline thickness / extra parameter)
 *  14  = refRadius (reflection light radius)
 */
object ShaderUniformId {
    const val COLOR = 0
    const val REF_INTENSITY = 1
    const val STRETCH = 2
    const val STRETCH_DIR = 4
    const val REF_ALBEDO = 6
    const val GLOW_INTENSITY = 7
    const val GLOW_SHARPNESS = 8
    const val GLOW_RADIUS = 9
    const val LIGHT_RADIUS = 10
    const val OUTER_SATURATION = 11
    const val CORNER_RADIUS_PIXEL = 12
    const val EXTRA = 13
    const val REF_RADIUS = 14
}

/**
 * All configurable GuidingLight shader parameters with Samsung defaults.
 * Extracted from GuidingLightConfig constructor defaults:
 *   GuidingLightConfig(RoundRect, ALL, (0.5,0.5), 1.92, color, 0.28, 0.28, 1.25, 36.0,
 *                      0.48, 1.82, 1.0, 0.07, 1.15, 0.9, 1.65, ?, DEFAULT, 60.0, 48.0)
 */
data class GuidingLightParams(
    /** Shape of the light effect: RoundRect or Circle */
    val shape: LightShape = LightShape.ROUND_RECT,

    /** Direction for round rect gradient. Determines which edge the light originates from. */
    val roundRectDirection: RoundRectDirection = RoundRectDirection.ALL,

    /** Light position in normalized coordinates (0..1). Default: center (0.5, 0.5) */
    val lightPos: PointF = PointF(0.5f, 0.5f),

    /** Primary light radius. Shader uniform ID=10. Default: 1.92 */
    @FloatRange(from = 0.0)
    val lightRadius: Float = 1.92f,

    /** Light color as ARGB int. Shader uniform ID=0 (set via Color uniform). */
    @ColorInt
    val lightColor: Int = DEFAULT_LIGHT_COLOR,

    /** Primary light intensity. Shader uniform ID=7 mapped via glowIntensity path. Default: 0.28 */
    @FloatRange(from = 0.0)
    val lightIntensity: Float = 0.28f,

    /** Glow effect intensity. Shader uniform ID=7. Default: 0.28 */
    @FloatRange(from = 0.0)
    val glowIntensity: Float = 0.28f,

    /** Glow effect radius. Shader uniform ID=9. Default: 1.25 */
    @FloatRange(from = 0.0)
    val glowRadius: Float = 1.25f,

    /** Glow sharpness. Shader uniform ID=8. Default: 36.0 */
    @FloatRange(from = 0.0)
    val glowSharpness: Float = 36.0f,

    /** Reflection light intensity. Shader uniform ID=1. Default: 0.48 */
    @FloatRange(from = 0.0)
    val reflLightIntensity: Float = 0.48f,

    /** Reflection light radius. Shader uniform ID=14. Default: 1.82 */
    @FloatRange(from = 0.0)
    val reflLightRadius: Float = 1.82f,

    /** Reflection albedo. Shader uniform ID=6. Default: 0.0 (hardcoded in Samsung code) */
    @FloatRange(from = 0.0, to = 1.0)
    val reflAlbedo: Float = 0.0f,

    /** Global luminance multiplier. Default: 1.0 */
    @FloatRange(from = 0.0)
    val globalLuminance: Float = 1.0f,

    /** Dither variation (noise amount). Default: 0.07 */
    @FloatRange(from = 0.0, to = 1.0)
    val ditherVariation: Float = 0.07f,

    /** Saturation of the effect. Default: 1.15 */
    @FloatRange(from = 0.0)
    val saturation: Float = 1.15f,

    /** Outer saturation. Shader uniform ID=11. Default: 0.9 */
    @FloatRange(from = 0.0)
    val outerSaturation: Float = 0.9f,

    /** Stretch factor for the effect. Shader uniform ID=2. Default: 1.65 */
    @FloatRange(from = 0.0)
    val stretch: Float = 1.65f,

    /** Stretch direction for lit areas. Shader uniform ID=4. Default: 0.0 (hardcoded) */
    val stretchDirLit: Float = 0.0f,

    /** Initial view alpha when effect is applied. Default: from GuidingLightConfig.r */
    @FloatRange(from = 0.0, to = 1.0)
    val initialViewAlpha: Float = 1.0f,

    /** Light movement mode. Default: DEFAULT (gyroscope-based) */
    val lightMovement: LightMovement = LightMovement.DEFAULT,

    /** Frame rate for the effect rendering. Default: 60.0 fps */
    val frameRate: Float = 60.0f,

    /** Outline thickness. Shader uniform ID=13 (extra). Default: 48.0 */
    @FloatRange(from = 0.0)
    val outlineThickness: Float = 48.0f
) {
    companion object {
        /** Default light color from Samsung: #4DFFFFFF (white with 30% alpha) */
        const val DEFAULT_LIGHT_COLOR: Int = 0x4DFFFFFF.toInt()

        /** Samsung preset: standard configuration (outerSaturation=1.15) */
        val SAMSUNG_STANDARD = GuidingLightParams(saturation = 1.15f)

        /** Samsung preset: enhanced configuration (outerSaturation=1.25) */
        val SAMSUNG_ENHANCED = GuidingLightParams(saturation = 1.25f)
    }
}

/** Shape of the guiding light effect */
enum class LightShape {
    ROUND_RECT,
    CIRCLE
}

/** Direction vector for round-rect light gradient */
enum class RoundRectDirection(val vector: PointF) {
    ALL(PointF(0.0f, 0.0f)),
    UP(PointF(0.0f, -1.0f)),
    RIGHT(PointF(1.0f, 0.0f)),
    DOWN(PointF(0.0f, 1.0f)),
    LEFT(PointF(-1.0f, 0.0f))
}

/** Light movement behavior */
enum class LightMovement {
    /** No movement - light stays in fixed position */
    NONE,
    /** Default gyroscope/sensor-based movement */
    DEFAULT
}

/** Corner radius side specification */
enum class CornerRadiusSide {
    TOP,
    BOTTOM,
    LEFT,
    RIGHT,
    ALL
}