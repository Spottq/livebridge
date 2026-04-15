package com.kakao.taxi.liveupdate

internal data class FlashlightCapability(
    val available: Boolean,
    val cameraId: String? = null,
    val maxStrengthLevel: Int = 1,
    val supportsStrengthControl: Boolean = false,
    val supportsFiveLevels: Boolean = false
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "available" to available,
            "supportsStrengthControl" to supportsStrengthControl,
            "supportsFiveLevels" to supportsFiveLevels,
            "maxStrengthLevel" to maxStrengthLevel
        )
    }
}
