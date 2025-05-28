package io.github.yaad.downloader_core

import android.webkit.WebSettings
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

@Serializable
data class ThreadPartInfo(
    val start: Long,
    val end: Long,
    var downloaded: Long,
    internal var lastKeyTime: Long = 0,
    internal var lastKeyDownLoad: Long = 0,
    var speed: Double = 0.0
)

@Serializable
data class DownloadCheckpoint(
    val url: String,
    val fileSize: Long,
    val parts: List<ThreadPartInfo>,
    val etag: String? = null // ETag for validation
)

enum class DownloadState {
    PENDING,
    DOWNLOADING,
    PAUSED,
    STOPPED,
    COMPLETED,
    ERROR,
    VALIDATING
}

data class DownloadStatus(
    val percent: Int,
    val totalDownloaded: Long,
    val parts: List<ThreadPartInfo>,
    val speed: Double,
    val state: DownloadState,
    val errorMessage: String? = ""
)

private data class ServerFileInfo(
    val supportsRange: Boolean,
    val fileSize: Long,
    val etag: String?
)

class HttpDownloadSession(
    private val url: String,
    private val path: String,
    private val headers: Map<String, String> = emptyMap(),
    private val threadCount: Int = 8
) : IDownloadSession {
    companion object {
        val ktorClient = HttpClient(CIO) {
            install(HttpTimeout) {
                requestTimeoutMillis = 30_000
                connectTimeoutMillis = 30_000
                socketTimeoutMillis = 3600_000
            }
        }


        private val defaultHeaders = mapOf("User-Agent" to getSystemUserAgent())

        private fun getSystemUserAgent(): String {
            return try {
                WebSettings.getDefaultUserAgent(getAppContext())
            } catch (e: Exception) {
                "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/113.0.0.0 Mobile Safari/537.36"
            }
        }

        private fun normalizeHeaderKey(key: String): String {
            return key.split("-").joinToString("-") { word ->
                word.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            }
        }
    }

    private val metaFile = File("$path.meta")
    private var checkpoint: DownloadCheckpoint? = null
    private var fd: Int = -1
    private var ptr: Long = 0L
    private var supportsRange = false
    private val speedUpdateTime: Long = 200 // milliseconds
    private var totalFileSize: Long = 0
    private var serverEtag: String? = null

    private val mergedHeaders: Map<String, String> = run {
        val normalizedHeaders = headers.mapKeys { (key, _) -> normalizeHeaderKey(key) }
        val normalizedDefaults = defaultHeaders.mapKeys { (key, _) -> normalizeHeaderKey(key) }
        normalizedDefaults + normalizedHeaders
    }

    override val total: Long
        get() = totalFileSize

    val fileName: String
        get() {
            return getFileName(url, "unknown")
        }

    @Volatile private var isPaused = false
    @Volatile private var isStopped = false
    @Volatile private var currentState = DownloadState.PENDING
    @Volatile private var currentErrorMessage: String? = null

    private val controlMutex = Mutex()
    private val downloadScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var downloadJobs: List<Job> = emptyList()
    private var progressReporterJob: Job? = null
    private val downloadListeners: HashSet<IDownloadListener> = HashSet()

    override suspend fun start(starResultListener: (e: Exception?) -> Unit) {
        if (currentState == DownloadState.DOWNLOADING || currentState == DownloadState.VALIDATING) {
            println("Download already in progress or validating.")
            starResultListener(IllegalStateException("Download already in progress or validating."))
            return
        }
        isStopped = false
        isPaused = false
        currentErrorMessage = null
        currentState = DownloadState.PENDING
        notifyStateChanged()

        val file = File(path)
        if (!file.exists()) {
            try {
                file.parentFile?.mkdirs()
                withContext(Dispatchers.IO) { file.createNewFile() }
            } catch (e: IOException) {
                currentState = DownloadState.ERROR
                currentErrorMessage = "Failed to create file: $path. ${e.message}"
                notifyStateChanged()
                starResultListener(IOException(currentErrorMessage, e))
                return
            }
        }

        val serverInfo: ServerFileInfo
        try {
            serverInfo = checkSupportForRangeAndGetInfoKtor(url)
        } catch (e: Exception) {
            currentState = DownloadState.ERROR
            currentErrorMessage = "Failed to get server file info: ${e.message}"
            notifyStateChanged()
            starResultListener(RuntimeException(currentErrorMessage, e))
            return
        }

        totalFileSize = serverInfo.fileSize
        supportsRange = serverInfo.supportsRange
        serverEtag = serverInfo.etag?.trim('"')

        if (!supportsRange && totalFileSize == -1L) { // File size unknown, likely chunked
            performChunkedDownloadKtor()
            starResultListener(null) // Indicate start was successful for chunked
            return
        }


        if (fd == -1 && totalFileSize > 0) {
            try {
                fd = NativeBridge.openFile(path)
            } catch (e: Exception) {
                currentState = DownloadState.ERROR
                currentErrorMessage = "Failed to open file for mmap: ${e.message}"
                notifyStateChanged()
                starResultListener(RuntimeException(currentErrorMessage, e))
                return
            }
        }
        if (ptr == 0L && totalFileSize > 0 && fd != -1) {
            try {
                ptr = NativeBridge.mmapFile(fd, totalFileSize)
            } catch (e: Exception) {
                currentState = DownloadState.ERROR
                currentErrorMessage = "Failed to mmap file: ${e.message}"
                notifyStateChanged()
                if (fd != -1) NativeBridge.closeFile(fd)
                fd = -1
                starResultListener(RuntimeException(currentErrorMessage, e))
                return
            }
        }


        var loadedCheckpoint = false
        if (supportsRange && metaFile.exists()) {
            val loaded = controlMutex.withLock { loadCheckpoint() }
            if (validateCheckpoint(loaded, serverInfo)) {
                checkpoint = loaded
                loadedCheckpoint = true
                println("Checkpoint loaded and validated.")
            } else {
                println("Checkpoint invalid or server file changed. Starting fresh.")
                metaFile.delete()
            }
        }

        if (!loadedCheckpoint) {
            val parts = if (supportsRange) {
                val partSize = totalFileSize / threadCount
                List(threadCount) { i ->
                    val start = i * partSize
                    val end = if (i == threadCount - 1) totalFileSize - 1 else (start + partSize - 1)
                    ThreadPartInfo(start, end, 0L)
                }
            } else {
                listOf(ThreadPartInfo(0, totalFileSize - 1, 0L)) // Single part for non-range or if threadCount is 1
            }
            checkpoint = DownloadCheckpoint(url, totalFileSize, parts, serverEtag)
        }

        val currentCheckpoint = checkpoint ?: run {
            currentState = DownloadState.ERROR
            currentErrorMessage = "Checkpoint could not be initialized."
            notifyStateChanged()
            starResultListener(IllegalStateException(currentErrorMessage))
            return@start
        }

        currentState = DownloadState.DOWNLOADING
        notifyStateChanged()

        progressReporterJob = downloadScope.launch {
            try {
                while (isActive) {
                    if (supportsRange && checkpoint != null) {
                        controlMutex.withLock {
                            saveCheckpoint()
                            updateThreadSpeedAndNotify()
                        }
                    } else if (!supportsRange && checkpoint != null) { // For chunked or non-range downloads
                        controlMutex.withLock {
                            updateThreadSpeedAndNotify() // Just update speed, no checkpoint saving here for chunked during download
                        }
                    }
                    delay(1000)
                }
            } catch (e: CancellationException) {
                println("Progress reporter cancelled.")
            } catch (e: Exception) {
                println("Progress reporter error: ${e.message}")
            } finally {
                if (supportsRange && checkpoint != null && (currentState == DownloadState.PAUSED || currentState == DownloadState.STOPPED || currentState == DownloadState.ERROR)) {
                    try {
                        controlMutex.withLock {
                            saveCheckpoint()
                            updateThreadSpeedAndNotify() // Final speed update
                        }
                    } catch (e: Exception) {
                        println("Error during final checkpoint save/speed update: ${e.message}")
                    }
                }
            }
        }

        try {
            downloadJobs = currentCheckpoint.parts.mapIndexed { index, part ->
                downloadScope.launch {
                    val startOffset = part.start + part.downloaded
                    if (startOffset > part.end && part.end != -1L) { // part.end can be -1 for chunked download initial part
                        println("Part ${index} (${part.start}-${part.end}) already completed.")
                        return@launch
                    }

                    var retryCount = 0
                    var lastError: IOException? = null

                    while (retryCount < 3 && isActive) {
                        if (isPaused) {
                            while (isPaused && isActive) {
                                delay(200)
                            }
                            if (!isActive) break
                        }
                        if (isStopped) break


                        var response: HttpResponse? = null
                        try {
                            response = ktorClient.prepareGet(url) {
                                mergedHeaders.forEach { (k, v) -> header(k, v) }
                                if (supportsRange) {
                                    header(HttpHeaders.Range, "bytes=$startOffset-${part.end}")
                                }
                                if (supportsRange && part.downloaded > 0 && currentCheckpoint.etag != null) {
                                    header(HttpHeaders.IfRange, currentCheckpoint.etag)
                                }
                            }.execute()

                            if (response.status.value !in 200..299) {
                                val errorMsg = "HTTP error: ${response.status} for part $index (range $startOffset-${part.end}). ETag used: ${currentCheckpoint.etag}"
                                println(errorMsg)
                                throw IOException(errorMsg)
                            }

                            val bodyChannel: ByteReadChannel = response.body()
                            val buffer = ByteArray(65536)
                            var mmapWriteOffset = startOffset

                            while (isActive) {
                                while (isPaused && isActive) {
                                    delay(200)
                                }
                                if (isStopped || !isActive) break

                                val read = bodyChannel.readAvailable(buffer, 0, buffer.size)
                                if (read == -1) break

                                if (ptr != 0L && supportsRange) { // Ensure mmap pointer is valid for ranged downloads
                                    for (j in 0 until read) {
                                        NativeBridge.writeByte(ptr, mmapWriteOffset + j, buffer[j])
                                    }
                                } else if (!supportsRange) {
                                    if (ptr != 0L) {
                                        for (j in 0 until read) {
                                            NativeBridge.writeByte(ptr, mmapWriteOffset + j, buffer[j])
                                        }
                                    } else {
                                        println("Warning: ptr is 0, cannot write with mmap for non-ranged part $index")
                                    }
                                }


                                mmapWriteOffset += read
                                controlMutex.withLock {
                                    part.downloaded += read
                                }
                            }
                            // If successfully completed, break retry loop
                            lastError = null // Clear last error
                            break
                        } catch (e: IOException) {
                            lastError = e
                            retryCount++
                            println("Retry attempt $retryCount for part $index after error: ${e.message}")
                            if (retryCount < 3 && isActive) {
                                delay(1000L * retryCount) // Exponential backoff for retries
                            }
                        } catch (e: CancellationException) {
                            println("Part $index download cancelled via coroutine cancellation.")
                            break // Exit retry loop
                        } catch (e: Exception) { // Catch other Ktor exceptions
                            lastError = IOException("Ktor client error: ${e.message}", e)
                            retryCount++
                            println("Retry attempt $retryCount for part $index after Ktor client error: ${e.message}")
                            if (retryCount < 3 && isActive) {
                                delay(1000L * retryCount)
                            }
                        }
                    }

                    if (retryCount == 3 && lastError != null) {
                        val errorMsg = "Part $index failed after 3 retries: ${lastError.message}"
                        println(errorMsg)
                        controlMutex.withLock {
                            if (currentErrorMessage == null) currentErrorMessage = errorMsg
                            else currentErrorMessage += "\n$errorMsg"
                        }
                    }
                }
            }

            starResultListener(null)
            downloadJobs.joinAll()

            // Post-download logic
            if (isStopped) {
                currentState = DownloadState.STOPPED
                println("Download was explicitly stopped.")
            } else if (downloadJobs.any { it.isCancelled } && currentErrorMessage != null && currentErrorMessage!!.contains("Critical part failure")) {
                currentState = DownloadState.ERROR
                println("Download error due to critical part failure.")
            }
            else if (downloadJobs.any { it.isCancelled }) {
                currentState = DownloadState.ERROR // Or some other cancellation
                if (currentErrorMessage == null) currentErrorMessage = "Download was cancelled or one or more parts failed."
                println(currentErrorMessage)
            }
            else if (currentErrorMessage != null) { // Errors accumulated during download parts
                currentState = DownloadState.ERROR
                println("Download finished with errors: $currentErrorMessage")
            }
            else {
                var allPartsCompleted = true
                currentCheckpoint.parts.forEach { part ->
                    val expectedToDownload = if (part.end == -1L) part.downloaded else (part.end - part.start + 1)
                    if (part.downloaded < expectedToDownload) {
                        allPartsCompleted = false
                    }
                }

                if (allPartsCompleted) {
                    if (ptr != 0L && currentCheckpoint.fileSize > 0) NativeBridge.msync(ptr, currentCheckpoint.fileSize)
                    if (fd != -1 && currentCheckpoint.fileSize > 0) NativeBridge.resizeFile(fd, currentCheckpoint.fileSize)

                    if (supportsRange && serverEtag != null) {
                        currentState = DownloadState.VALIDATING
                        notifyStateChanged()
                        println("Validating ETag post-download...")
                        try {
                            val latestServerInfo = checkSupportForRangeAndGetInfoKtor(url) // Re-fetch with Ktor
                            if (serverEtag == latestServerInfo.etag?.trim('"')) {
                                println("ETag validation successful.")
                                currentState = DownloadState.COMPLETED
                            } else {
                                val errorMsg = "ETag mismatch after download! Initial: $serverEtag, Current: ${latestServerInfo.etag}. File might have changed or download corrupted."
                                println(errorMsg)
                                currentErrorMessage = errorMsg
                                currentState = DownloadState.ERROR
                            }
                        } catch (e: Exception) {
                            val errorMsg = "Failed to re-fetch server info for ETag validation: ${e.message}"
                            println(errorMsg)
                            currentErrorMessage = errorMsg
                            currentState = DownloadState.ERROR
                        }
                    } else {
                        currentState = DownloadState.COMPLETED
                    }

                    if (currentState == DownloadState.COMPLETED && supportsRange) {
                        controlMutex.withLock { metaFile.delete() }
                    }
                } else { // Not all parts completed, but no explicit stop or cancellation recorded above.
                    currentState = DownloadState.ERROR
                    if (currentErrorMessage == null) currentErrorMessage = "One or more parts failed to download completely."
                    println(currentErrorMessage)
                }
            }
            notifyStateChanged()
            if (currentState == DownloadState.COMPLETED) {
                downloadListeners.forEach { it.onComplete(this) }
            }


        } catch (e: Exception) {
            if (e is CancellationException) {
                println("Download scope cancelled by user or error.")
                if (!isStopped) currentState = DownloadState.STOPPED // If not stopped explicitly, treat as stopped/cancelled
                if (currentErrorMessage == null) currentErrorMessage = "Download cancelled."
            } else {
                currentState = DownloadState.ERROR
                currentErrorMessage = currentErrorMessage ?: "Download failed: ${e.message}"
                println(currentErrorMessage)
            }
            notifyStateChanged()
        } finally {
            progressReporterJob?.cancelAndJoin()

            if (currentState != DownloadState.COMPLETED && currentState != DownloadState.PAUSED) {
                if (ptr != 0L && checkpoint != null && checkpoint!!.fileSize > 0) {
                    NativeBridge.munmap(ptr, checkpoint!!.fileSize)
                }
                if (fd != -1) {
                    NativeBridge.closeFile(fd)
                }
                ptr = 0L
                fd = -1
            } else if (currentState == DownloadState.COMPLETED) { // Ensure cleanup on completion too
                if (ptr != 0L && checkpoint != null && checkpoint!!.fileSize > 0) {
                    NativeBridge.munmap(ptr, checkpoint!!.fileSize)
                }
                if (fd != -1) {
                    NativeBridge.closeFile(fd)
                }
                ptr = 0L
                fd = -1
            }


            if (supportsRange && checkpoint != null && currentState != DownloadState.COMPLETED) {
                try {
                    controlMutex.withLock {
                        saveCheckpoint()
                        updateThreadSpeedAndNotify()
                    }
                } catch (e: Exception) {
                    println("Error during final checkpoint save in finally block: ${e.message}")
                }
            }
            updateStateOnJobsExit()
            if (currentState == DownloadState.ERROR || currentState == DownloadState.STOPPED) {
                notifyStateChanged()
            }
            println("Download session ended with state: $currentState")
        }
    }


    private suspend fun performChunkedDownloadKtor() {
        currentState = DownloadState.DOWNLOADING
        currentErrorMessage = null
        notifyStateChanged()
        val tempFile = File("$path.tmp_chunked")
        var outputStream: FileOutputStream? = null
        var response: HttpResponse? = null

        try {
            response = ktorClient.prepareGet(url) {
                mergedHeaders.forEach { (k, v) -> header(k, v) }
            }.execute()

            if (response.status.value !in 200..299) {
                val errorMsg = "HTTP error: ${response.status} for chunked download."
                currentErrorMessage = errorMsg
                throw IOException(errorMsg)
            }

            val bodyChannel: ByteReadChannel = response.body()
            outputStream = FileOutputStream(tempFile)
            val buffer = ByteArray(65536) // 64KB buffer
            var downloadedBytes = 0L


            controlMutex.withLock {
                checkpoint = DownloadCheckpoint(url, -1L, listOf(ThreadPartInfo(0, -1, 0L)), serverEtag)
            }


            while (downloadScope.isActive) {
                while (isPaused && downloadScope.isActive) {
                    delay(200)
                }
                if (isStopped || !downloadScope.isActive) break

                val read = bodyChannel.readAvailable(buffer, 0, buffer.size)
                if (read == -1) break // End of stream

                outputStream.write(buffer, 0, read)
                downloadedBytes += read

                controlMutex.withLock {
                    totalFileSize = downloadedBytes
                    checkpoint?.parts?.firstOrNull()?.let { part ->
                        part.downloaded = downloadedBytes
                    }
                    checkpoint = checkpoint?.copy(fileSize = downloadedBytes)
                    updateThreadSpeedAndNotify()
                }
            }
            withContext(Dispatchers.IO) {
                outputStream.flush()
            }

            // After download loop finishes
            if (isStopped) {
                currentState = DownloadState.STOPPED
                tempFile.delete()
                println("Chunked download stopped.")
            } else if (!downloadScope.isActive && !isStopped) { // Cancelled not via stop()
                currentState = DownloadState.STOPPED // Or ERROR, depending on how it was cancelled
                currentErrorMessage = currentErrorMessage ?: "Chunked download cancelled."
                tempFile.delete()
                println("Chunked download cancelled.")
            }
            else if (bodyChannel.isClosedForRead) {
                tempFile.renameTo(File(path))
                currentState = DownloadState.COMPLETED
                controlMutex.withLock {
                    checkpoint = checkpoint?.copy(fileSize = downloadedBytes, parts = listOf(ThreadPartInfo(0, downloadedBytes -1, downloadedBytes)))
                }
                println("Chunked download completed. Total bytes: $downloadedBytes")
            } else {
                // Stream didn't finish, not stopped, not paused, but loop exited. Possible error.
                currentState = DownloadState.ERROR
                currentErrorMessage = currentErrorMessage ?: "Chunked download exited loop unexpectedly."
                tempFile.delete()
                println(currentErrorMessage)
            }

        } catch (e: Exception) {
            tempFile.delete()
            if (e is CancellationException) {
                currentState = DownloadState.STOPPED // Or what makes sense for cancellation
                currentErrorMessage = currentErrorMessage ?: "Chunked download cancelled during operation."
            } else {
                currentState = DownloadState.ERROR
                currentErrorMessage = currentErrorMessage ?: "Chunked download failed: ${e.message}"
            }
            println("Chunked download exception: $currentErrorMessage")
        } finally {
            try {
                outputStream?.close()
            } catch (e: IOException) {
                println("Error closing output stream for chunked download: ${e.message}")
            }
            response?.cancel() // Ensure Ktor response is fully processed
            notifyStateChanged()
            if (currentState == DownloadState.COMPLETED) {
                downloadListeners.forEach { it.onComplete(this) }
            }
            updateStateOnJobsExit() // Final speed update to 0
            println("Chunked download session ended with state: $currentState")
        }
    }

    private fun updateStateOnJobsExit() {
        val currentParts = checkpoint?.parts ?: return
        val isActuallyStopped = (currentState == DownloadState.STOPPED || currentState == DownloadState.PAUSED || currentState == DownloadState.ERROR || currentState == DownloadState.COMPLETED)

        currentParts.forEach { part ->
            if (isActuallyStopped || part.downloaded >= (part.end - part.start + 1) && part.end != -1L) {
                part.speed = 0.0
            }
            // Update lastKeyTime and lastKeyDownLoad only if speed is reset, or periodically
            // This helps in resuming speed calculation correctly.
            // If we always set it here, speed might always be zero on next calc if no new bytes.
            // The main updateThreadSpeed handles periodic updates.
            if (part.speed == 0.0) {
                part.lastKeyDownLoad = part.downloaded
                part.lastKeyTime = System.currentTimeMillis()
            }
        }
        // Potentially notify progress one last time if state indicates no more active download
        if (isActuallyStopped) {
            notifyProgress()
        }
    }


    private fun validateCheckpoint(cp: DownloadCheckpoint?, serverInfo: ServerFileInfo): Boolean {
        if (cp == null) return false
        if (cp.url != url) {
            println("Checkpoint URL mismatch: CP='${cp.url}', Current='${url}'")
            return false
        }
        // For chunked downloads, serverInfo.fileSize might be -1.
        // If cp.fileSize is also -1 or 0 (initial state for a chunked cp), it's okay.
        // If cp.fileSize > 0, it means we have a partially downloaded chunked file.
        // Server might still report -1 or a new total size if it became known.
        // This validation is primarily for range-supported downloads.
        if (serverInfo.fileSize != -1L && cp.fileSize != serverInfo.fileSize) {
            println("Checkpoint fileSize mismatch: CP='${cp.fileSize}', Server='${serverInfo.fileSize}' (Skipping check if server reports -1)")
            // If server reports a definitive size and it doesn't match, it's an issue
            if (cp.fileSize > 0 ) return false // Only fail if checkpoint had a size and it mismatches a known server size
        }

        val cpEtagTrimmed = cp.etag?.trim('"')
        val serverEtagTrimmed = serverInfo.etag?.trim('"')

        if (serverEtagTrimmed != null) { // Server provides an ETag
            if (cpEtagTrimmed == null) { // Checkpoint doesn't have one
                println("Server ETag available ('$serverEtagTrimmed') but not in checkpoint. Treating as invalid for safety if checkpoint has progress.")
                // If there's downloaded progress in checkpoint, an ETag mismatch is risky
                return if (cp.parts.any { it.downloaded > 0 }) false else true // Allow if no progress made yet
            }
            if (cpEtagTrimmed != serverEtagTrimmed) {
                println("Checkpoint ETag mismatch: CP='$cpEtagTrimmed', Server='$serverEtagTrimmed'")
                return false
            }
        } else { // Server does not provide an ETag
            if (cpEtagTrimmed != null) {
                println("Checkpoint has ETag ('$cpEtagTrimmed') but server does not provide one. Continuing cautiously.")
                // This might be acceptable, but a bit risky if the file changed without ETag update
            }
        }
        return true
    }

    private fun updateThreadSpeedAndNotify() {
        val currentParts = checkpoint?.parts ?: return
        val currentTime = System.currentTimeMillis()
        var madeChanges = false

        currentParts.forEach { part ->
            val timeDiff = currentTime - part.lastKeyTime
            if (timeDiff > speedUpdateTime) {
                val deltaDownload = part.downloaded - part.lastKeyDownLoad
                if (timeDiff > 0) {
                    val newSpeed = deltaDownload / (timeDiff / 1000.0)
                    if (part.speed != newSpeed) madeChanges = true
                    part.speed = newSpeed
                } else if (deltaDownload == 0L && part.speed != 0.0) {
                    part.speed = 0.0 // No download, no time passed (or very small), speed is 0
                    madeChanges = true
                }
                part.lastKeyTime = currentTime
                part.lastKeyDownLoad = part.downloaded
            } else if (part.downloaded > 0 && part.end != -1L && part.downloaded >= (part.end - part.start + 1)) { // Part completed
                if (part.speed != 0.0) {
                    part.speed = 0.0
                    madeChanges = true
                }
            } else if (currentState != DownloadState.DOWNLOADING && part.speed != 0.0) {
                // If not downloading (paused, stopped, error, completed), speed should be 0
                part.speed = 0.0
                madeChanges = true
                part.lastKeyTime = currentTime // Update time to prevent immediate recalc on resume
                part.lastKeyDownLoad = part.downloaded
            }
        }

        if (madeChanges || downloadListeners.isNotEmpty()) { // Always notify if listeners are present, or if speed/data changed
            notifyProgress()
        }
    }

    private fun notifyProgress() {
        if (downloadListeners.isNotEmpty()) {
            val status = getStatus() // Get a consistent snapshot
            downloadListeners.forEach { it.onProgress(this) } // Pass `this` (IDownloadSession)
        }
    }
    private fun notifyStateChanged() {
        // This can be expanded to call specific listener methods based on state
        // For now, it's a general signal that state might have changed, and getStatus() will reflect it.
        // You could also call notifyProgress() here if a state change implies progress should be re-evaluated.
        // Example:
        // when(currentState) {
        //     DownloadState.PAUSED -> downloadListeners.forEach { it.onPause(this) }
        //     DownloadState.COMPLETED -> downloadListeners.forEach { it.onComplete(this) }
        //     ... etc.
        // }
        // For simplicity, often getStatus() is polled or progress updates handle notifications.
        // However, direct state notifications are good too.
    }


    @Synchronized // Keep synchronized as it's accessed from different contexts
    override fun getStatus(): DownloadStatus {
        val c = checkpoint // Capture volatile read
        var totalDownloadedAllParts = 0L
        var totalSpeedAllParts = 0.0
        val partsInfoCopy = mutableListOf<ThreadPartInfo>()

        val currentCheckpointParts = c?.parts
        if (currentCheckpointParts != null) {
            for (part in currentCheckpointParts) {
                totalDownloadedAllParts += part.downloaded
                totalSpeedAllParts += part.speed
                partsInfoCopy.add(part.copy()) // Create a defensive copy for the status
            }
        } else if (totalFileSize > 0 && !supportsRange && currentState != DownloadState.COMPLETED && currentState != DownloadState.ERROR && currentState != DownloadState.PENDING) {
            // Fallback for non-ranged if checkpoint is somehow null but download is active/paused
            // This might occur if performChunkedDownload is running and checkpoint isn't fully formed yet,
            // or if it's a simple, non-resumable download without explicit parts.
            try {
                val currentFile = File(path)
                if (currentFile.exists()) {
                    totalDownloadedAllParts = currentFile.length()
                }
            } catch (e: Exception) { /* ignore, stick to 0 */ }
        } else if (currentState == DownloadState.COMPLETED) {
            totalDownloadedAllParts = totalFileSize // Ensure this is accurate
        }


        val percent = if (totalFileSize <= 0L) {
            if (currentState == DownloadState.COMPLETED && totalDownloadedAllParts > 0) 100
            else 0
        } else {
            (totalDownloadedAllParts * 100.0 / totalFileSize).toInt().coerceIn(0,100)
        }


        return DownloadStatus(
            percent,
            totalDownloadedAllParts,
            partsInfoCopy,
            totalSpeedAllParts.coerceAtLeast(0.0), // Ensure speed isn't negative
            currentState,
            currentErrorMessage ?: ""
        )
    }

    override suspend fun pause() {
        if (currentState == DownloadState.DOWNLOADING) {
            isPaused = true // Signal threads to pause
            // Threads will see `isPaused` and enter their delay loop.
            // State change after ensuring threads acknowledge pause or after a short delay.
            currentState = DownloadState.PAUSED
            println("Download pause requested. Waiting for tasks to acknowledge pause.")
            // Give a moment for download loops to enter pause state
            delay(speedUpdateTime + 100) // Wait slightly longer than speed update interval

            if (supportsRange && checkpoint != null) {
                controlMutex.withLock {
                    saveCheckpoint() // Save progress made until pause
                    updateThreadSpeedAndNotify() // Update speeds to 0 and notify
                }
            } else if (!supportsRange && checkpoint != null) { // For chunked/non-ranged
                controlMutex.withLock {
                    // For chunked, checkpoint saving isn't as critical on pause, but speed update is.
                    updateThreadSpeedAndNotify()
                }
            }
            downloadListeners.forEach { it.onPause(this) }
            println("Download paused.")
            notifyStateChanged()
        }
    }

    override suspend fun resume() {
        if (currentState == DownloadState.PAUSED) {
            isPaused = false
            currentErrorMessage = null // Clear previous non-critical errors if any
            currentState = DownloadState.DOWNLOADING
            // Threads will exit their pause loop.
            // Speed calculation will resume in updateThreadSpeed.
            // No need to restart jobs, they are just waiting.
            // However, if file/mmap resources were released on pause, they need re-init.
            // Current implementation does not release mmap on pause.

            // Ensure checkpoint parts have their lastKeyTime reset so speed calc is fresh
            checkpoint?.parts?.forEach {
                it.lastKeyTime = System.currentTimeMillis() // Reset for immediate speed calc
                it.lastKeyDownLoad = it.downloaded // Ensures no false jump in speed
            }

            println("Download resumed.")
            downloadListeners.forEach { it.onResume(this, path) }
            notifyStateChanged()
        }
    }

    override suspend fun stop() {
        if (currentState != DownloadState.STOPPED && currentState != DownloadState.COMPLETED) {
            val previousState = currentState
            isPaused = false // Ensure not stuck in pause logic
            isStopped = true // Signal threads and loops to stop
            currentState = DownloadState.STOPPED
            println("Download stopping procedures initiated...")
            notifyStateChanged() // Notify early about stopping intent

            // Create a custom CancellationException to distinguish from other cancellations
            val stopCancellationReason = CancellationException("Download stopped by user")

            // Cancel individual download jobs first
            downloadJobs.forEach { job ->
                if (job.isActive) {
                    job.cancel(stopCancellationReason)
                }
            }
            // Cancel the progress reporter
            progressReporterJob?.takeIf { it.isActive }?.cancel(stopCancellationReason)

            // Attempt to wait for jobs to complete cancellation gracefully
            try {
                withTimeout(5000) { // 5 seconds timeout for graceful shutdown
                    downloadJobs.joinAll() // Wait for download parts
                    progressReporterJob?.join() // Wait for progress reporter
                }
            } catch (e: TimeoutCancellationException) {
                println("Timeout waiting for download tasks to stop gracefully. Forcing supervisor job cancellation.")
                // Forcefully cancel the entire scope if timeout, though SupervisorJob children handle their own cancellation.
                // This ensures the scope itself is aware it should clean up.
                downloadScope.cancel(stopCancellationReason) // This will cancel any remaining active children
            } catch (e: Exception) {
                println("Exception during job joinAll/join on stop: ${e.message}")
            }


            // Resources cleanup (mmap, fd)
            // This was in the finally block of start(), but good to ensure it here too for stop()
            if (ptr != 0L && checkpoint != null && checkpoint!!.fileSize > 0) {
                NativeBridge.munmap(ptr, checkpoint!!.fileSize)
            }
            if (fd != -1) {
                NativeBridge.closeFile(fd)
            }
            ptr = 0L
            fd = -1

            // Save checkpoint if supported and download was in a state that warrants it
            if (supportsRange && checkpoint != null && previousState != DownloadState.COMPLETED && previousState != DownloadState.ERROR) {
                try {
                    controlMutex.withLock {
                        saveCheckpoint()
                        updateThreadSpeedAndNotify() // Final save and speed update to 0
                    }
                } catch (e: Exception) {
                    println("Error saving checkpoint during stop: ${e.message}")
                }
            } else if (!supportsRange && previousState == DownloadState.DOWNLOADING) {
                // For chunked downloads, if stopped, the .tmp_chunked file might be deleted or kept.
                // Current performChunkedDownloadKtor deletes .tmp_chunked on stop.
                // No checkpoint to save here usually for chunked, but speeds should be zeroed.
                controlMutex.withLock { updateThreadSpeedAndNotify() }
            }

            // downloadListeners.forEach { it.onStop(this) } // Add if you have onStop listener
            println("Download fully stopped.")
            // State is already STOPPED, notifyStateChanged() might have been called earlier or can be called again.
        }
    }

    override suspend fun remove() {
        stop() // Ensure download is stopped and resources are released
        // Wait a moment for stop() to complete its actions if it runs asynchronously in parts
        delay(200)

        val targetFile = File(path)
        val tempChunkedFile = File("$path.tmp_chunked")

        if (targetFile.exists()) {
            if (targetFile.delete()) {
                println("Main file deleted: ${targetFile.path}")
            } else {
                println("Failed to delete main file: ${targetFile.path}")
            }
        }
        if (metaFile.exists()) {
            if (metaFile.delete()) {
                println("Meta file deleted: ${metaFile.path}")
            } else {
                println("Failed to delete meta file: ${metaFile.path}")
            }
        }
        if (tempChunkedFile.exists()) {
            if (tempChunkedFile.delete()) {
                println("Temp chunked file deleted: ${tempChunkedFile.path}")
            } else {
                println("Failed to delete temp chunked file: ${tempChunkedFile.path}")
            }
        }

        checkpoint = null
        totalFileSize = 0L
        serverEtag = null
        currentErrorMessage = null
        // Reset state to PENDING or a new 'REMOVED' state if you prefer
        currentState = DownloadState.PENDING // Or a new specific state like 'DELETED'
        isPaused = false
        isStopped = false // Reset flags for potential reuse if this object instance is kept

        // Clear native resources again just in case stop() didn't fully finalize before remove() was called
        if (ptr != 0L) { // Should be 0L if stop() worked
            println("Warning: ptr was not 0 during remove. Attempting munmap.")
            // We need a size for munmap. If checkpoint is null, this is problematic.
            // This indicates a potential issue in resource lifecycle.
            // For safety, we might not be able to call munmap if size info is lost.
        }
        if (fd != -1) { // Should be -1 if stop() worked
            println("Warning: fd was not -1 during remove. Attempting closeFile.")
            NativeBridge.closeFile(fd)
            fd = -1
        }
        ptr = 0L // Ensure they are reset

        println("Download session removed for URL: $url")
        notifyStateChanged() // Notify that the state has changed (e.g., to PENDING or DELETED)
    }


    private suspend fun checkSupportForRangeAndGetInfoKtor(targetUrl: String): ServerFileInfo {
        // Use Ktor's HEAD request first if possible, or GET and then consume/close body quickly.
        // For simplicity, using GET and relying on Ktor to handle body efficiently if not consumed.
        // A HEAD request is better: ktorClient.head(targetUrl) { ... }
        var response: HttpResponse? = null
        try {
            response = ktorClient.get(targetUrl) {
                method = HttpMethod.Head // Request HEAD to get headers without body
                mergedHeaders.forEach { (k, v) -> header(k, v) }
            }

            // If HEAD fails or is not allowed, try GET
            if (response.status.value !in 200..299 || response.status.value == 405) { // 405 Method Not Allowed
                response.cancel()
                response = ktorClient.get(targetUrl) { // Fallback to GET
                    mergedHeaders.forEach { (k, v) -> header(k, v) }
                }
            }


            if (response.status.value !in 200..299) {
                throw IOException("HTTP Response code: ${response.status} from ${response.call.request.method.value} $targetUrl")
            }

            val rangeHeader = response.headers[HttpHeaders.AcceptRanges]?.lowercase()
            val currentEtag = response.headers[HttpHeaders.ETag]
            val transferEncoding = response.headers[HttpHeaders.TransferEncoding]?.lowercase()
            val isChunked = transferEncoding == "chunked"
            val contentLength = response.headers[HttpHeaders.ContentLength]?.toLongOrNull()

            val effectiveSupportsRange = rangeHeader == "bytes" && contentLength != null && contentLength > 0 && !isChunked

            if (isChunked) {
                return ServerFileInfo(false, -1L, currentEtag) // File size unknown for chunked
            }
            if (contentLength == null) {
                // If not chunked and no content-length, it's problematic.
                // Could be a stream of indeterminate length. Treat as not supporting range and size unknown for now.
                // Or throw an error if a content length is strictly expected.
                println("Warning: Content-Length header missing and not chunked for $targetUrl. Range support disabled, size unknown.")
                return ServerFileInfo(false, -1L, currentEtag)
                // throw IOException("Failed to get Content-Length and not chunked for $targetUrl.")
            }

            return ServerFileInfo(effectiveSupportsRange, contentLength, currentEtag)
        } finally {
            response?.cancel() // Ensure the response body is consumed and connection is closed
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    private fun saveCheckpoint() {
        if (checkpoint == null) {
            println("Attempted to save a null checkpoint.")
            return
        }
        if (!supportsRange) { // Don't save checkpoints for non-range (e.g. chunked) downloads
            // println("Skipping checkpoint save for non-range download.")
            return
        }
        val tempMetaFile = File("$path.meta.tmp")
        try {
            metaFile.parentFile?.mkdirs() // Ensure directory exists
            val bytes = Cbor.encodeToByteArray(checkpoint!!)
            tempMetaFile.writeBytes(bytes)

            // Atomic rename is preferred
            if (metaFile.exists()) {
                if (!metaFile.delete()) {
                    println("Warning: Could not delete old meta file: ${metaFile.path}. Attempting overwrite via copy.")
                    // Fallback to copy and delete if rename fails (e.g. different filesystems or permissions)
                    try {
                        tempMetaFile.copyTo(metaFile, overwrite = true)
                        if (!tempMetaFile.delete()) {
                            println("Warning: Could not delete temp meta file after copy: ${tempMetaFile.path}")
                        }
                        // println("Checkpoint saved successfully via copy (old delete failed).")
                        return
                    } catch (copyEx: Exception) {
                        println("Error saving checkpoint via copy fallback: ${copyEx.message}")
                        // Attempt to clean up temp file if copy failed
                        if (tempMetaFile.exists() && !tempMetaFile.delete()) {
                            println("Warning: Could not delete temp meta file after failed copy: ${tempMetaFile.path}")
                        }
                        return // Exit, save failed
                    }
                }
            }
            // Try rename first
            if (tempMetaFile.renameTo(metaFile)) {
                // println("Checkpoint saved successfully via rename.")
            } else {
                // Fallback to copy if rename fails
                println("Warning: Rename failed for meta file. Attempting copy.")
                tempMetaFile.copyTo(metaFile, overwrite = true)
                tempMetaFile.delete() // Clean up temp file
                // println("Checkpoint saved successfully via copy (rename failed).")
            }
        } catch (e: Exception) {
            println("Error saving checkpoint: ${e.message}")
            // Ensure temp file is deleted on error
            if (tempMetaFile.exists()) {
                tempMetaFile.delete()
            }
        }
    }


    @OptIn(ExperimentalSerializationApi::class)
    private fun loadCheckpoint(): DownloadCheckpoint? {
        if (!metaFile.exists()) return null
        return try {
            val bytes = metaFile.readBytes()
            if (bytes.isEmpty()) {
                println("Meta file is empty. Deleting corrupted meta file.")
                metaFile.delete()
                return null
            }
            Cbor.decodeFromByteArray<DownloadCheckpoint>(bytes)
        } catch (e: Exception) {
            println("Failed to load checkpoint: ${e.message}. Deleting corrupted meta file.")
            metaFile.delete()
            null
        }
    }

    override fun addDownloadListener(listener: IDownloadListener) {
        downloadListeners.add(listener)
    }

    override fun removeDownloadListener(listener: IDownloadListener) {
        downloadListeners.remove(listener)
    }

    fun recoverFilePath(): String { // This seems like a utility, maybe not part of core download logic
        return metaFile.absolutePath
    }
}