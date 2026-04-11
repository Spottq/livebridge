package com.waymo.carapp.liveupdate

import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.TrafficStats
import java.util.concurrent.ConcurrentHashMap

internal data class NetworkTrafficSnapshot(
    val rxBytes: Long,
    val txBytes: Long
)

internal class NetworkSpeedMonitor(context: Context) {
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val validInterfaces = ConcurrentHashMap<Network, String>()
    private var started = false

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities
        ) {
            super.onCapabilitiesChanged(network, networkCapabilities)
            updateNetwork(
                network = network,
                capabilities = networkCapabilities,
                linkProperties = connectivityManager.getLinkProperties(network)
            )
        }

        override fun onLinkPropertiesChanged(network: Network, linkProperties: LinkProperties) {
            super.onLinkPropertiesChanged(network, linkProperties)
            updateNetwork(
                network = network,
                capabilities = connectivityManager.getNetworkCapabilities(network),
                linkProperties = linkProperties
            )
        }

        override fun onLost(network: Network) {
            super.onLost(network)
            validInterfaces.remove(network)
        }
    }

    fun start() {
        if (started) {
            return
        }
        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
            .addTransportType(NetworkCapabilities.TRANSPORT_ETHERNET)
            .build()
        connectivityManager.registerNetworkCallback(request, networkCallback)
        connectivityManager.allNetworks.forEach { network ->
            updateNetwork(
                network = network,
                capabilities = connectivityManager.getNetworkCapabilities(network),
                linkProperties = connectivityManager.getLinkProperties(network)
            )
        }
        started = true
    }

    fun stop() {
        if (!started) {
            return
        }
        runCatching { connectivityManager.unregisterNetworkCallback(networkCallback) }
        validInterfaces.clear()
        started = false
    }

    fun readSnapshot(): NetworkTrafficSnapshot {
        var totalRx = 0L
        var totalTx = 0L

        validInterfaces.values.distinct().forEach { interfaceName ->
            val rxBytes = TrafficStats.getRxBytes(interfaceName)
            val txBytes = TrafficStats.getTxBytes(interfaceName)
            if (rxBytes != TrafficStats.UNSUPPORTED.toLong()) {
                totalRx += rxBytes
            }
            if (txBytes != TrafficStats.UNSUPPORTED.toLong()) {
                totalTx += txBytes
            }
        }

        return NetworkTrafficSnapshot(rxBytes = totalRx, txBytes = totalTx)
    }

    private fun updateNetwork(
        network: Network,
        capabilities: NetworkCapabilities?,
        linkProperties: LinkProperties?
    ) {
        if (capabilities == null || linkProperties == null) {
            validInterfaces.remove(network)
            return
        }

        if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) {
            validInterfaces.remove(network)
            return
        }

        val isPhysicalTransport =
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)

        if (!isPhysicalTransport) {
            validInterfaces.remove(network)
            return
        }

        val interfaceName = linkProperties.interfaceName
        if (interfaceName.isNullOrBlank()) {
            validInterfaces.remove(network)
            return
        }

        validInterfaces[network] = interfaceName
    }
}
