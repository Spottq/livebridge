package com.nowbar.api.effects

import androidx.annotation.ColorInt

/**
 * All Samsung Now Bar color constants extracted from Samsung Health resources.
 *
 * Sources:
 *   - res/values/colors.xml (light theme)
 *   - res/values-night/colors.xml (dark/night theme)
 *   - GuidingLightEffect internal lightmap color: #4DFFFFFF
 */
object NowBarColors {

    // ---------------------------------------------------------------------------
    // Now Bar progress colors (from tracker_sport_now_bar_* resources)
    // ---------------------------------------------------------------------------

    /** Now Bar progress indicator - light theme. Resource: tracker_sport_now_bar_progress */
    @ColorInt
    const val PROGRESS_LIGHT: Int = 0xFF0FCF6E.toInt()

    /** Now Bar progress indicator - dark/night theme. Resource: tracker_sport_now_bar_progress */
    @ColorInt
    const val PROGRESS_DARK: Int = 0xFF39D489.toInt()

    /** Now Bar progress track (background) - light theme. Resource: tracker_sport_now_bar_progress_track */
    @ColorInt
    const val PROGRESS_TRACK_LIGHT: Int = 0xFFE1E1E1.toInt()

    /** Now Bar progress track (background) - dark/night theme. Resource: tracker_sport_now_bar_progress_track */
    @ColorInt
    const val PROGRESS_TRACK_DARK: Int = 0x4D000000

    // ---------------------------------------------------------------------------
    // GuidingLight effect internal colors
    // ---------------------------------------------------------------------------

    /** Default lightmap overlay color used in GuidingLightEffect. White at 30% alpha. */
    @ColorInt
    const val GUIDING_LIGHT_OVERLAY: Int = 0x4DFFFFFF

    // ---------------------------------------------------------------------------
    // Resource ID references (from public.xml)
    // ---------------------------------------------------------------------------

    /** Public resource ID for tracker_sport_now_bar_progress color */
    const val RES_ID_PROGRESS: Int = 0x7f061662

    /** Public resource ID for tracker_sport_now_bar_progress_track color */
    const val RES_ID_PROGRESS_TRACK: Int = 0x7f061663
}