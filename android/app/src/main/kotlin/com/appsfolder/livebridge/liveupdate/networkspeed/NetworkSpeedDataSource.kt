package com.kakao.taxi.liveupdate.networkspeed

import android.net.TrafficStats

data class NetworkTrafficData(
    val rxBytes: Long,
    val txBytes: Long
)

data class NetworkSpeedSample(
    val downloadBytesPerSecond: Long = 0L,
    val uploadBytesPerSecond: Long = 0L
) {
    val totalBytesPerSecond: Long
        get() = downloadBytesPerSecond + uploadBytesPerSecond
}

object NetworkSpeedDataSource {
    fun getTrafficData(): NetworkTrafficData {
        val totalRx = TrafficStats.getTotalRxBytes()
            .takeIf { it != TrafficStats.UNSUPPORTED.toLong() }
            ?.coerceAtLeast(0L)
            ?: 0L
        val totalTx = TrafficStats.getTotalTxBytes()
            .takeIf { it != TrafficStats.UNSUPPORTED.toLong() }
            ?.coerceAtLeast(0L)
            ?: 0L
        return NetworkTrafficData(rxBytes = totalRx, txBytes = totalTx)
    }
}
