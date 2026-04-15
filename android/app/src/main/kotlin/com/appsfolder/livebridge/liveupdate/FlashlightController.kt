package com.kakao.taxi.liveupdate

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import kotlin.math.abs
import kotlin.math.roundToInt

internal class FlashlightController(context: Context) {
    private val appContext = context.applicationContext
    private val cameraManager = appContext.getSystemService(CameraManager::class.java)

    fun getCapability(): FlashlightCapability {
        return runCatching {
            val cameraId = resolveFlashlightCameraId() ?: return FlashlightCapability(available = false)
            val characteristics = cameraManager.getCameraCharacteristics(cameraId)
            val maxStrengthLevel =
                characteristics.get(CameraCharacteristics.FLASH_INFO_STRENGTH_MAXIMUM_LEVEL)
                    ?.coerceAtLeast(1) ?: 1
            FlashlightCapability(
                available = true,
                cameraId = cameraId,
                maxStrengthLevel = maxStrengthLevel,
                supportsStrengthControl = maxStrengthLevel > 1,
                supportsFiveLevels = maxStrengthLevel >= FLASHLIGHT_LEVEL_COUNT
            )
        }.getOrElse {
            FlashlightCapability(available = false)
        }
    }

    fun apply(enabled: Boolean, requestedLevelIndex: Int): FlashlightCapability {
        val capability = getCapability()
        val cameraId = capability.cameraId ?: return capability
        if (enabled) {
            if (capability.supportsStrengthControl) {
                cameraManager.turnOnTorchWithStrengthLevel(
                    cameraId,
                    resolveStrengthLevel(capability, requestedLevelIndex)
                )
            } else {
                cameraManager.setTorchMode(cameraId, true)
            }
        } else {
            cameraManager.setTorchMode(cameraId, false)
        }
        return capability
    }

    fun registerTorchCallback(callback: CameraManager.TorchCallback) {
        cameraManager.registerTorchCallback(appContext.mainExecutor, callback)
    }

    fun unregisterTorchCallback(callback: CameraManager.TorchCallback) {
        cameraManager.unregisterTorchCallback(callback)
    }

    fun resolveStrengthLevel(capability: FlashlightCapability, requestedLevelIndex: Int): Int {
        if (!capability.supportsStrengthControl) {
            return 1
        }
        val levelIndex = requestedLevelIndex.coerceIn(0, FLASHLIGHT_LEVEL_COUNT - 1)
        return buildStrengthSteps(capability.maxStrengthLevel)[levelIndex]
    }

    fun resolveLevelIndexForStrength(capability: FlashlightCapability, torchStrength: Int): Int {
        if (!capability.supportsFiveLevels) {
            return DEFAULT_LEVEL_INDEX
        }
        val strength = torchStrength.coerceIn(1, capability.maxStrengthLevel.coerceAtLeast(1))
        val steps = buildStrengthSteps(capability.maxStrengthLevel)
        var bestIndex = DEFAULT_LEVEL_INDEX
        var bestDistance = Int.MAX_VALUE
        for (index in steps.indices) {
            val distance = abs(steps[index] - strength)
            if (distance < bestDistance) {
                bestDistance = distance
                bestIndex = index
            }
        }
        return bestIndex
    }

    private fun resolveFlashlightCameraId(): String? {
        val cameraIds = cameraManager.cameraIdList ?: return null
        return cameraIds
            .mapNotNull { cameraId ->
                val characteristics = runCatching {
                    cameraManager.getCameraCharacteristics(cameraId)
                }.getOrNull() ?: return@mapNotNull null
                val hasFlash = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                if (!hasFlash) {
                    return@mapNotNull null
                }
                val lensFacing =
                    characteristics.get(CameraCharacteristics.LENS_FACING)
                        ?: CameraCharacteristics.LENS_FACING_EXTERNAL
                Triple(cameraId, lensFacingPriority(lensFacing), cameraId)
            }
            .sortedBy { it.second }
            .firstOrNull()
            ?.first
    }

    private fun buildStrengthSteps(maxStrengthLevel: Int): IntArray {
        val maxLevel = maxStrengthLevel.coerceAtLeast(1)
        val steps = IntArray(FLASHLIGHT_LEVEL_COUNT)
        var previous = 0
        for (index in 0 until FLASHLIGHT_LEVEL_COUNT) {
            val remainingSlots = (FLASHLIGHT_LEVEL_COUNT - 1) - index
            val raw = if (index == FLASHLIGHT_LEVEL_COUNT - 1) {
                maxLevel
            } else {
                1 + (((maxLevel - 1).toDouble() * index) / (FLASHLIGHT_LEVEL_COUNT - 1))
                    .roundToInt()
            }
            val minAllowed = previous + 1
            val maxAllowed = (maxLevel - remainingSlots).coerceAtLeast(minAllowed)
            val normalized = raw.coerceIn(minAllowed, maxAllowed)
            steps[index] = normalized
            previous = normalized
        }
        steps[FLASHLIGHT_LEVEL_COUNT - 1] = maxLevel
        return steps
    }

    private fun lensFacingPriority(lensFacing: Int): Int {
        return when (lensFacing) {
            CameraCharacteristics.LENS_FACING_BACK -> 0
            CameraCharacteristics.LENS_FACING_EXTERNAL -> 1
            CameraCharacteristics.LENS_FACING_FRONT -> 2
            else -> 3
        }
    }

    companion object {
        const val FLASHLIGHT_LEVEL_COUNT = 5
        const val DEFAULT_LEVEL_INDEX = 4
    }
}
