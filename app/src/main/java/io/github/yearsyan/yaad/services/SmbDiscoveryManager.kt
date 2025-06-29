package io.github.yearsyan.yaad.services

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import java.util.concurrent.Executor
import java.util.concurrent.Executors

object SmbDiscoveryManager {

    private const val TAG = "MdnsDiscovery"
    private var nsdManager: NsdManager? = null
    private var discoveryListener: NsdManager.DiscoveryListener? = null
    private val executor: Executor = Executors.newSingleThreadExecutor()
    private val services: HashSet<NsdServiceInfo> = HashSet()

    fun initialize(context: Context) {
        if (nsdManager != null) {
            return
        }
        nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    }

    fun startDiscovery() {
        if (discoveryListener != null) {
            return
        }
        if (nsdManager == null) {
            Log.e(TAG, "MdnsDiscoveryManager is not initialized.")
            return
        }

        stopDiscovery()

        discoveryListener =
            object : NsdManager.DiscoveryListener {
                override fun onDiscoveryStarted(serviceType: String) {
                    Log.d(TAG, "Discovery started: $serviceType")
                }

                override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                    Log.d(TAG, "Service found: $serviceInfo")

                    nsdManager?.registerServiceInfoCallback(
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
                                services.add(serviceInfo)
                            }
                        }
                    )
                }

                override fun onServiceLost(serviceInfo: NsdServiceInfo) {
                    val removed =
                        services.filter {
                            it.hostAddresses.any { host ->
                                serviceInfo.hostAddresses.contains(host)
                            }
                        }
                    services.removeAll(removed)
                }

                override fun onDiscoveryStopped(serviceType: String) {
                    Log.d(TAG, "Discovery stopped: $serviceType")
                }

                override fun onStartDiscoveryFailed(
                    serviceType: String,
                    errorCode: Int
                ) {
                    Log.e(TAG, "Discovery start failed: $errorCode")
                    stopDiscovery()
                }

                override fun onStopDiscoveryFailed(
                    serviceType: String,
                    errorCode: Int
                ) {
                    Log.e(TAG, "Discovery stop failed: $errorCode")
                }
            }

        var serviceType = "_smb._tcp."
        nsdManager?.discoverServices(
            serviceType,
            NsdManager.PROTOCOL_DNS_SD,
            discoveryListener!!
        )
    }

    fun stopDiscovery() {
        discoveryListener?.let {
            nsdManager?.stopServiceDiscovery(it)
            Log.d(TAG, "Discovery stopped manually")
        }
        services.clear()
        discoveryListener = null
    }
}
