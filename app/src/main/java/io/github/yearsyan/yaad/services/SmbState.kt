package io.github.yearsyan.yaad.services

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import io.github.yearsyan.yaad.services.SmbDiscoveryManager.stopDiscovery
import io.github.yearsyan.yaad.utils.isGlobalIPv6Available
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.Inet4Address
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import kotlin.collections.filter

class SmbState(val context: Context, val coroutineScope: CoroutineScope) {

    private val executor: Executor = Executors.newSingleThreadExecutor()
    private val nsdManager =
        context.getSystemService(Context.NSD_SERVICE) as NsdManager

    private var discoveryListener: NsdManager.DiscoveryListener =
        object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(serviceType: String) {
            }

            override fun onServiceFound(serviceInfo: NsdServiceInfo) {

                nsdManager.registerServiceInfoCallback(
                    serviceInfo,
                    executor,
                    object : NsdManager.ServiceInfoCallback {
                        override fun onServiceInfoCallbackRegistrationFailed(
                            errorCode: Int
                        ) {}

                        override fun onServiceInfoCallbackUnregistered() {}

                        override fun onServiceLost() {}

                        override fun onServiceUpdated(
                            serviceInfo: NsdServiceInfo
                        ) {
                            if (!isGlobalIPv6Available()) {
                                serviceInfo.hostAddresses = serviceInfo.hostAddresses.filter { it is Inet4Address }
                                if (serviceInfo.hostAddresses.isEmpty()) {
                                    return
                                }
                            }
                            coroutineScope.launch(Dispatchers.Main) {
                                val currentServices = services.value.filterNot { it.serviceName == serviceInfo.serviceName }
                                services.value = currentServices.plus(serviceInfo)
                            }
                        }
                    }
                )
            }

            override fun onServiceLost(serviceInfo: NsdServiceInfo) {
                coroutineScope.launch(Dispatchers.Main) {
                    services.value = services.value.filterNot { it.serviceName == serviceInfo.serviceName }
                }
            }

            override fun onDiscoveryStopped(serviceType: String) {

            }

            override fun onStartDiscoveryFailed(
                serviceType: String,
                errorCode: Int
            ) {
                stopDiscovery()
            }

            override fun onStopDiscoveryFailed(
                serviceType: String,
                errorCode: Int
            ) {
            }
        }
    val services = mutableStateOf<List<NsdServiceInfo>>(emptyList())

    fun start() {
        nsdManager.discoverServices(
            "_smb._tcp.",
            NsdManager.PROTOCOL_DNS_SD,
            discoveryListener
        )
    }

    fun stop() {
        nsdManager.stopServiceDiscovery(discoveryListener)
    }
}
