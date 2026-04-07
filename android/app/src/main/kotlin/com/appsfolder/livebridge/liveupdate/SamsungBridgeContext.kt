package com.kakao.taxi.liveupdate

internal data class SamsungBridgeContext(
    val enabled: Boolean,
    val reparsePayload: SamsungReparsePayload?,
    val hasNativeOrSamsungProgress: Boolean,
    val hasCustomRemoteCard: Boolean
) {
    companion object {
        fun disabled(sourceHasNativeProgress: Boolean): SamsungBridgeContext {
            return SamsungBridgeContext(
                enabled = false,
                reparsePayload = null,
                hasNativeOrSamsungProgress = sourceHasNativeProgress,
                hasCustomRemoteCard = false
            )
        }
    }
}
