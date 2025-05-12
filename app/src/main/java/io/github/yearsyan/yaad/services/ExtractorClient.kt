package io.github.yearsyan.yaad.services

import android.content.*
import android.os.*
import android.util.Log
import io.github.yearsyan.yaad.IExtractorService
import io.github.yearsyan.yaad.model.MediaResult
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ExtractorClient
private constructor(private val applicationContext: Context) {

    private var extractorService: IExtractorService? = null
    private var isBound = AtomicBoolean(false)
    private val serviceIntent =
        Intent(applicationContext, ExtractorService::class.java)
    private val withConnectionStateListener = HashSet<(Boolean) -> Unit>()

    private val serviceConnection =
        object : ServiceConnection {
            override fun onServiceConnected(
                name: ComponentName?,
                service: IBinder?
            ) {
                extractorService = IExtractorService.Stub.asInterface(service)
                isBound.set(true)
                synchronized(withConnectionStateListener) {
                    withConnectionStateListener.forEach { it(true) }
                    withConnectionStateListener.clear()
                }
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                extractorService = null
                isBound.set(false)
                synchronized(withConnectionStateListener) {
                    withConnectionStateListener.forEach { it(false) }
                    withConnectionStateListener.clear()
                }
            }

            override fun onBindingDied(name: ComponentName?) {
                onServiceDisconnected(name)
            }

            override fun onNullBinding(name: ComponentName?) {
                synchronized(withConnectionStateListener) {
                    withConnectionStateListener.forEach { it(false) }
                    withConnectionStateListener.clear()
                }
                isBound.set(false)
            }
        }

    fun connect(): Boolean {
        if (isBound.compareAndSet(false, false)) {
            try {
                return applicationContext.bindService(
                    serviceIntent,
                    serviceConnection,
                    Context.BIND_AUTO_CREATE
                )
            } catch (sec: SecurityException) {
                Log.e(
                    "ExtractorClient",
                    "Failed to bind service due to SecurityException",
                    sec
                )
                // Handle permission issues if service requires them or is exported incorrectly
            } catch (e: Exception) {
                Log.e("ExtractorClient", "Failed to bind service", e)
            }
        } else {
            Log.d("ExtractorClient", "Already bound or attempting to bind.")
            return true
        }
        return false
    }

    fun disconnect() {
        if (isBound.compareAndSet(true, false)) {
            try {
                applicationContext.unbindService(serviceConnection)
            } finally {
                extractorService = null
            }
        }
    }

    fun withConnectionState(t: (Boolean) -> Unit) {
        if (isBound.get()) {
            t(true)
        } else {
            synchronized(withConnectionStateListener) {
                withConnectionStateListener.add(t)
            }
        }
    }

    suspend fun extractMedia(
        url: String,
        options: Map<Any?, Any?>?
    ): MediaResult? {
        if (!isBound.get() || extractorService == null) {
            return null
        }

        return withContext(Dispatchers.IO) {
            try {
                Log.d(
                    "ExtractorClient",
                    "Calling extractDownloadMedia for: $url"
                )
                extractorService?.extractDownloadMedia(
                    url,
                    options?.toMutableMap()
                )
            } catch (e: RemoteException) {
                Log.e(
                    "ExtractorClient",
                    "RemoteException during extractMedia call",
                    e
                )
                null
            }
        }
    }

    companion object {
        @Volatile private var INSTANCE: ExtractorClient? = null

        fun initialize(context: Context) {
            synchronized(this) {
                if (INSTANCE == null) {
                    INSTANCE = ExtractorClient(context.applicationContext)
                    Log.d("ExtractorClient", "Singleton initialized.")
                }
            }
        }

        fun getInstance(): ExtractorClient {
            return INSTANCE
                ?: throw IllegalStateException(
                    "ExtractorClient must be initialized first."
                )
        }
    }
}
