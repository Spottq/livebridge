package com.kakao.taxi.liveupdate

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
    val txBytes: Long,
    val interfaces: Map<String, NetworkInterfaceTraffic>
)

internal data class NetworkInterfaceTraffic(
    val rxBytes: Long,
    val txBytes: Long,
    val transport: NetworkTransport
)

internal enum class NetworkTransport {
    WIFI,
    MOBILE,
    ETHERNET
}

internal class NetworkSpeedMonitor(context: Context) {
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val validInterfaces = ConcurrentHashMap<Network, TrackedNetworkInterface>()
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
        val interfaces = linkedMapOf<String, NetworkInterfaceTraffic>()

        validInterfaces.values.distinctBy { it.name }.forEach { trackedInterface ->
            val rxBytes = TrafficStats.getRxBytes(trackedInterface.name)
            val txBytes = TrafficStats.getTxBytes(trackedInterface.name)
            if (rxBytes == TrafficStats.UNSUPPORTED.toLong() &&
                txBytes == TrafficStats.UNSUPPORTED.toLong()
            ) {
                return@forEach
            }
            interfaces[trackedInterface.name] = NetworkInterfaceTraffic(
                rxBytes = rxBytes.coerceAtLeast(0L),
                txBytes = txBytes.coerceAtLeast(0L),
                transport = trackedInterface.transport
            )
        }

        return NetworkTrafficSnapshot(
            rxBytes = interfaces.values.sumOf { it.rxBytes },
            txBytes = interfaces.values.sumOf { it.txBytes },
            interfaces = interfaces
        )
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

        val transport = when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ->
                NetworkTransport.WIFI
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ->
                NetworkTransport.MOBILE
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) ->
                NetworkTransport.ETHERNET
            else -> null
        }
        if (transport == null) {
            validInterfaces.remove(network)
            return
        }

        val interfaceName = linkProperties.interfaceName
        if (interfaceName.isNullOrBlank()) {
            validInterfaces.remove(network)
            return
        }

        validInterfaces[network] = TrackedNetworkInterface(
            name = interfaceName,
            transport = transport
        )
    }

    private data class TrackedNetworkInterface(
        val name: String,
        val transport: NetworkTransport
    )
}
