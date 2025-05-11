package io.github.yaad.downloader_core

import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
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
import okhttp3.OkHttpClient
import okhttp3.Request

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
        private val client =
            OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build()
    }

    private val metaFile = File("$path.meta")
    private var checkpoint: DownloadCheckpoint? = null
    private var fd: Int = -1
    private var ptr: Long = 0L
    private var supportsRange = false
    private val speedUpdateTime: Long =
        200 // milliseconds, used by updateThreadSpeed
    private var totalFileSize: Long = 0
    private var serverEtag: String? = null

    override val total: Long
        get() = totalFileSize

    @Volatile private var isPaused = false
    @Volatile private var isStopped = false
    @Volatile private var currentState = DownloadState.PENDING
    @Volatile private var currentErrorMessage: String? = null

    private val controlMutex = Mutex()
    private val downloadScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var downloadJobs: List<Job> = emptyList()
    private var progressReporterJob: Job? = null
    private val downloadListeners: HashSet<IDownloadListener> = HashSet()

    override suspend fun start() {
        if (
            currentState == DownloadState.DOWNLOADING ||
                currentState == DownloadState.VALIDATING
        ) {
            println("Download already in progress or validating.")
            return
        }
        isStopped = false
        isPaused = false
        currentErrorMessage = null
        currentState = DownloadState.PENDING

        val file = File(path)
        if (!file.exists()) {
            try {
                file.parentFile?.mkdirs()
                withContext(Dispatchers.IO) { file.createNewFile() }
            } catch (e: IOException) {
                currentState = DownloadState.ERROR
                currentErrorMessage =
                    "Failed to create file: $path. ${e.message}"
                throw IOException(currentErrorMessage, e)
            }
        }

        val serverInfo: ServerFileInfo
        try {
            serverInfo = checkSupportForRangeAndGetInfo(url)
        } catch (e: Exception) {
            currentState = DownloadState.ERROR
            currentErrorMessage = "Failed to get server file info: ${e.message}"
            throw RuntimeException(currentErrorMessage, e)
        }

        totalFileSize = serverInfo.fileSize
        supportsRange = serverInfo.supportsRange
        serverEtag = serverInfo.etag?.trim('"')

        if (!supportsRange && totalFileSize == -1L) {
            performChunkedDownload()
            return
        }

        if (fd == -1 && totalFileSize > 0)
            fd = NativeBridge.openFile(path) // Only open if there's a size
        if (ptr == 0L && totalFileSize > 0)
            ptr = NativeBridge.mmapFile(fd, totalFileSize)

        var loadedCheckpoint = false
        if (supportsRange && metaFile.exists()) {
            val loaded = controlMutex.withLock { loadCheckpoint() }
            if (validateCheckpoint(loaded, serverInfo)) {
                checkpoint = loaded
                loadedCheckpoint = true
                println("Checkpoint loaded and validated.")
            } else {
                println(
                    "Checkpoint invalid or server file changed. Starting fresh."
                )
                metaFile.delete()
            }
        }

        if (!loadedCheckpoint) {
            val parts =
                if (supportsRange) {
                    val partSize = totalFileSize / threadCount
                    List(threadCount) { i ->
                        val start = i * partSize
                        val end =
                            if (i == threadCount - 1) totalFileSize - 1
                            else (start + partSize - 1)
                        ThreadPartInfo(start, end, 0L)
                    }
                } else {
                    listOf(ThreadPartInfo(0, totalFileSize - 1, 0L))
                }
            checkpoint =
                DownloadCheckpoint(url, totalFileSize, parts, serverEtag)
        }

        val currentCheckpoint =
            checkpoint
                ?: run {
                    currentState = DownloadState.ERROR
                    currentErrorMessage = "Checkpoint could not be initialized."
                    throw IllegalStateException(currentErrorMessage)
                }

        currentState = DownloadState.DOWNLOADING

        progressReporterJob =
            downloadScope.launch {
                try {
                    while (isActive) {
                        if (supportsRange && checkpoint != null) {
                            controlMutex.withLock {
                                saveCheckpoint()
                                updateThreadSpeed()
                            }
                        }
                        delay(1000)
                    }
                } catch (e: CancellationException) {
                    println("Progress reporter cancelled.")
                } catch (e: Exception) {
                    println("Progress reporter error: ${e.message}")
                } finally {
                    if (
                        supportsRange &&
                            checkpoint != null &&
                            (currentState == DownloadState.PAUSED ||
                                currentState == DownloadState.STOPPED ||
                                currentState == DownloadState.ERROR)
                    ) {
                        try {
                            controlMutex.withLock {
                                saveCheckpoint()
                                updateThreadSpeed() // Final speed update
                            }
                        } catch (e: Exception) {
                            println(
                                "Error during final checkpoint save/speed update: ${e.message}"
                            )
                        }
                    }
                }
            }

        try {
            downloadJobs =
                currentCheckpoint.parts.mapIndexed { index, part ->
                    downloadScope.launch {
                        val startOffset = part.start + part.downloaded
                        if (startOffset > part.end && part.end != -1L) {
                            println(
                                "Part ${index} (${part.start}-${part.end}) already completed."
                            )
                            return@launch
                        }

                        val reqBuilder = Request.Builder().url(url)
                        if (supportsRange) {
                            reqBuilder.addHeader(
                                "Range",
                                "bytes=$startOffset-${part.end}"
                            )
                        }
                        headers.forEach { (k, v) -> reqBuilder.addHeader(k, v) }
                        if (
                            supportsRange &&
                                part.downloaded > 0 &&
                                currentCheckpoint.etag != null
                        ) {
                            reqBuilder.addHeader(
                                "If-Range",
                                currentCheckpoint.etag
                            )
                        }

                        val req = reqBuilder.build()

                        try {
                            client.newCall(req).execute().use { response ->
                                if (!response.isSuccessful) {
                                    val errorMsg =
                                        "HTTP error: ${response.code} for part $index (range $startOffset-${part.end}). ETag used: ${currentCheckpoint.etag}"
                                    println(errorMsg)
                                    controlMutex.withLock {
                                        currentErrorMessage =
                                            currentErrorMessage ?: errorMsg
                                    }
                                    throw IOException(errorMsg)
                                }
                                val body =
                                    response.body
                                        ?: throw IOException(
                                            "No response body for part $index (range $startOffset-${part.end})"
                                        )
                                val inputStream = body.byteStream()
                                val buffer = ByteArray(65536)
                                var mmapWriteOffset = startOffset

                                while (isActive) {
                                    while (isPaused && isActive) {
                                        delay(200)
                                    }
                                    if (isStopped || !isActive) break

                                    val read = inputStream.read(buffer)
                                    if (read == -1) break

                                    if (
                                        ptr != 0L
                                    ) { // Ensure mmap pointer is valid
                                        for (j in 0 until read) {
                                            NativeBridge.writeByte(
                                                ptr,
                                                mmapWriteOffset + j,
                                                buffer[j]
                                            )
                                        }
                                    } else if (
                                        !supportsRange
                                    ) { // Fallback for single part non-mmap (should not happen if
                                        // mmap is primary for non-chunked)
                                        // This case needs a proper FileOutputStream for the single
                                        // part if mmap is not used.
                                        // For now, assuming mmap is used if ptr is available.
                                        println(
                                            "Warning: ptr is 0, cannot write with mmap for part $index"
                                        )
                                    }

                                    mmapWriteOffset += read
                                    controlMutex.withLock {
                                        part.downloaded += read
                                    }
                                }
                            }
                        } catch (e: IOException) {
                            if (isActive) {
                                val errorMsg =
                                    "Error downloading part $index (${part.start}-${part.end}): ${e.message}"
                                println(errorMsg)
                                controlMutex.withLock {
                                    currentErrorMessage =
                                        currentErrorMessage ?: errorMsg
                                }
                                throw e
                            }
                        } catch (e: CancellationException) {
                            println("Part $index download cancelled.")
                            // Don't rethrow cancellation, let the job complete as cancelled.
                        }
                    }
                }

            downloadJobs.joinAll()

            if (
                !isStopped && !downloadJobs.any { it.isCancelled }
            ) { // Check if any job was cancelled or if globally stopped
                var allPartsCompleted = true
                currentCheckpoint.parts.forEach { part ->
                    if (part.downloaded < (part.end - part.start + 1)) {
                        allPartsCompleted = false
                    }
                }

                if (allPartsCompleted) {
                    if (ptr != 0L)
                        NativeBridge.msync(ptr, currentCheckpoint.fileSize)
                    if (fd != -1)
                        NativeBridge.resizeFile(fd, currentCheckpoint.fileSize)

                    if (supportsRange && serverEtag != null) {
                        currentState = DownloadState.VALIDATING
                        println("Validating ETag post-download...")
                        try {
                            val latestServerInfo =
                                checkSupportForRangeAndGetInfo(url)
                            if (
                                serverEtag == latestServerInfo.etag?.trim('"')
                            ) {
                                println("ETag validation successful.")
                                currentState = DownloadState.COMPLETED
                            } else {
                                val errorMsg =
                                    "ETag mismatch after download! Initial: $serverEtag, Current: ${latestServerInfo.etag}. File might have changed or download corrupted."
                                println(errorMsg)
                                currentErrorMessage = errorMsg
                                currentState = DownloadState.ERROR
                            }
                        } catch (e: Exception) {
                            val errorMsg =
                                "Failed to re-fetch server info for ETag validation: ${e.message}"
                            println(errorMsg)
                            currentErrorMessage = errorMsg
                            currentState = DownloadState.ERROR
                        }
                    } else {
                        currentState = DownloadState.COMPLETED
                    }

                    if (
                        currentState == DownloadState.COMPLETED && supportsRange
                    ) {
                        controlMutex.withLock { metaFile.delete() }
                    }

                    if (currentState == DownloadState.COMPLETED) {
                        downloadListeners.forEach { it.onComplete() }
                    }
                } else if (!isStopped) {
                    currentState = DownloadState.ERROR
                    if (currentErrorMessage == null)
                        currentErrorMessage =
                            "One or more parts failed to download completely."
                }
            } else { // Some jobs cancelled, not explicitly stopped
                currentState = DownloadState.ERROR
                if (currentErrorMessage == null)
                    currentErrorMessage =
                        "Download was cancelled or one or more parts failed."
            }
        } catch (e: Exception) {
            if (e is CancellationException) {
                println("Download scope cancelled by user or error.")
                if (!isStopped)
                    currentState =
                        DownloadState
                            .ERROR // If not stopped explicitly, it's an error
                if (currentErrorMessage == null)
                    currentErrorMessage = "Download cancelled."
            } else {
                currentState = DownloadState.ERROR
                currentErrorMessage =
                    currentErrorMessage ?: "Download failed: ${e.message}"
                println(currentErrorMessage)
            }
        } finally {
            progressReporterJob?.cancelAndJoin()

            if (
                currentState != DownloadState.COMPLETED &&
                    currentState != DownloadState.PAUSED
            ) {
                if (ptr != 0L && checkpoint != null)
                    NativeBridge.munmap(ptr, checkpoint!!.fileSize)
                if (fd != -1) NativeBridge.closeFile(fd)
                ptr = 0L
                fd = -1
            }

            if (
                supportsRange &&
                    checkpoint != null &&
                    currentState != DownloadState.COMPLETED
            ) {
                controlMutex.withLock {
                    saveCheckpoint()
                    updateThreadSpeed() // Final save and speed update
                }
            }
        }
    }

    private suspend fun performChunkedDownload() {
        currentState = DownloadState.DOWNLOADING
        currentErrorMessage = null
        val tempFile = File("$path.tmp_chunked")

        try {
            val request =
                Request.Builder()
                    .url(url)
                    .apply { headers.forEach { (k, v) -> addHeader(k, v) } }
                    .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errorMsg =
                        "HTTP error: ${response.code} for chunked download."
                    currentErrorMessage = errorMsg
                    throw IOException(errorMsg)
                }

                val input =
                    response.body?.byteStream()
                        ?: run {
                            val errorMsg =
                                "No response body for chunked download."
                            currentErrorMessage = errorMsg
                            throw IOException(errorMsg)
                        }
                FileOutputStream(tempFile).use { output ->
                    val buffer = ByteArray(65536)
                    var downloadedBytes = 0L
                    while (downloadScope.isActive) {
                        while (isPaused && downloadScope.isActive) {
                            delay(200)
                        }
                        if (isStopped || !downloadScope.isActive) break

                        val read = input.read(buffer)
                        if (read == -1) break
                        output.write(buffer, 0, read)
                        downloadedBytes += read
                        totalFileSize = downloadedBytes
                        controlMutex.withLock { // Protect checkpoint update
                            checkpoint =
                                DownloadCheckpoint(
                                    url,
                                    downloadedBytes,
                                    listOf(
                                        ThreadPartInfo(0, -1, downloadedBytes)
                                    ),
                                    null
                                )
                            // For chunked download, speed calculation might be simpler or global
                            updateThreadSpeed() // Update speed for the single "part"
                        }
                    }
                }
            }

            updateStateOnJobsExit()
            if (isStopped) {
                currentState = DownloadState.STOPPED
                tempFile.delete()
            } else if (!downloadScope.isActive && !isStopped) {
                currentState = DownloadState.STOPPED
                currentErrorMessage =
                    currentErrorMessage ?: "Chunked download cancelled."
                tempFile.delete()
            } else {
                tempFile.renameTo(File(path))
                currentState = DownloadState.COMPLETED
            }
        } catch (e: Exception) {
            tempFile.delete()
            if (e is CancellationException) {
                currentState = DownloadState.STOPPED
                currentErrorMessage =
                    currentErrorMessage
                        ?: "Chunked download cancelled during operation."
            } else {
                currentState = DownloadState.ERROR
                currentErrorMessage =
                    currentErrorMessage
                        ?: "Chunked download failed: ${e.message}"
            }
            println(currentErrorMessage)
        }
    }

    private fun updateStateOnJobsExit() {
        val currentParts =
            checkpoint?.parts
                ?: return // checkpoint read under controlMutex by caller
        currentParts.forEach { part ->
            part.speed = 0.0
            part.lastKeyDownLoad = part.downloaded
            part.lastKeyTime = System.currentTimeMillis()
        }
    }

    private fun validateCheckpoint(
        cp: DownloadCheckpoint?,
        serverInfo: ServerFileInfo
    ): Boolean {
        if (cp == null) return false
        if (cp.url != url) {
            println("Checkpoint URL mismatch: CP='${cp.url}', Current='${url}'")
            return false
        }
        if (cp.fileSize != serverInfo.fileSize) {
            println(
                "Checkpoint fileSize mismatch: CP='${cp.fileSize}', Server='${serverInfo.fileSize}'"
            )
            return false
        }
        val cpEtagTrimmed = cp.etag?.trim('"')
        val serverEtagTrimmed = serverInfo.etag?.trim('"')

        if (serverEtagTrimmed != null) {
            if (cpEtagTrimmed == null) {
                println(
                    "Server ETag available ('$serverEtagTrimmed') but not in checkpoint."
                )
                return false
            }
            if (cpEtagTrimmed != serverEtagTrimmed) {
                println(
                    "Checkpoint ETag mismatch: CP='$cpEtagTrimmed', Server='$serverEtagTrimmed'"
                )
                return false
            }
        }
        return true
    }

    private fun updateThreadSpeed() {
        val currentParts = checkpoint?.parts ?: return
        val currentTime = System.currentTimeMillis()
        currentParts.forEach { part ->
            if (currentTime - part.lastKeyTime > speedUpdateTime) {
                val deltaDownload = part.downloaded - part.lastKeyDownLoad
                val deltaTimeMillis = currentTime - part.lastKeyTime
                if (deltaTimeMillis > 0) {
                    part.speed = deltaDownload / (deltaTimeMillis / 1000.0)
                } else if (deltaDownload == 0L) {
                    part.speed = 0.0
                }
                part.lastKeyTime = currentTime
                part.lastKeyDownLoad = part.downloaded
            } else if (
                part.downloaded == (part.end - part.start + 1) &&
                    part.downloaded > 0
            ) { // Part completed
                part.speed = 0.0
            }
        }
        if (downloadListeners.isNotEmpty()) {
            downloadListeners.forEach { it.onProgress() }
        }
    }

    @Synchronized
    override fun getStatus(): DownloadStatus {

        val c = checkpoint
        var totalDownloadedAllParts = 0L
        var totalSpeedAllParts = 0.0
        val partsInfoCopy = mutableListOf<ThreadPartInfo>()

        val currentCheckpointParts = c?.parts
        if (currentCheckpointParts != null) {
            for (part in currentCheckpointParts) {
                totalDownloadedAllParts += part.downloaded
                totalSpeedAllParts += part.speed
                partsInfoCopy.add(part.copy())
            }
        } else if (
            totalFileSize > 0 &&
                !supportsRange &&
                currentState != DownloadState.COMPLETED &&
                currentState != DownloadState.ERROR
        ) {
            totalDownloadedAllParts = File(path).length()
        } else if (currentState == DownloadState.COMPLETED) {
            totalDownloadedAllParts = totalFileSize
        }

        val percent =
            if (totalFileSize <= 0L) 0
            else (totalDownloadedAllParts * 100 / totalFileSize).toInt()

        return DownloadStatus(
            percent,
            totalDownloadedAllParts,
            partsInfoCopy,
            totalSpeedAllParts,
            currentState,
            currentErrorMessage ?: "" // Provide current error message
        )
    }

    override suspend fun pause() {
        if (currentState == DownloadState.DOWNLOADING) {
            isPaused = true
            currentState = DownloadState.PAUSED
            println("Download paused.")
            if (supportsRange && checkpoint != null) {
                controlMutex.withLock {
                    saveCheckpoint()
                    updateThreadSpeed() // Update speed before saving paused state
                    downloadListeners.forEach { it.onPause() }
                }
            }
        }
    }

    override suspend fun resume() {
        if (currentState == DownloadState.PAUSED) {
            isPaused = false
            currentErrorMessage = null // Clear previous error on resume
            currentState = DownloadState.DOWNLOADING
            downloadListeners.forEach { it.onResume(path) }
        }
    }

    override suspend fun stop() { // Return a Job representing the stop operation
        if (currentState != DownloadState.STOPPED) {
            isPaused = false
            isStopped = true // Set before cancelling to allow loops to exit
            val previousState = currentState
            currentState = DownloadState.STOPPED
            println("Download stopping procedures initiated...")

            downloadJobs.forEach {
                it.cancel(CancellationException("Download stopped by user"))
            }
            progressReporterJob?.cancel(
                CancellationException("Download stopped by user")
            )

            // Wait for jobs to complete cancellation, with a timeout
            try {
                withTimeout(5000) { // 5 seconds timeout for graceful shutdown
                    downloadJobs.joinAll()
                    progressReporterJob?.join()
                }
            } catch (e: TimeoutCancellationException) {
                println(
                    "Timeout waiting for download tasks to stop gracefully."
                )
            }

            // Resources cleanup
            if (ptr != 0L && checkpoint != null) {
                NativeBridge.munmap(ptr, checkpoint!!.fileSize)
            }
            if (fd != -1) {
                NativeBridge.closeFile(fd)
            }
            ptr = 0L
            fd = -1

            if (
                supportsRange &&
                    checkpoint != null &&
                    previousState != DownloadState.COMPLETED
            ) {
                try {
                    controlMutex.withLock {
                        saveCheckpoint()
                        updateThreadSpeed() // Final save and speed update
                    }
                } catch (e: Exception) {

                    println("Error saving checkpoint during stop: ${e.message}")
                }
            }
            println("Download fully stopped.")
        }
    }

    override suspend fun remove() {
        stop()
        File(path).delete()
        metaFile.delete()
        checkpoint = null
        totalFileSize = 0L
        serverEtag = null
        currentErrorMessage = null
    }

    private fun checkSupportForRangeAndGetInfo(
        targetUrl: String
    ): ServerFileInfo {
        val reqBuilder = Request.Builder().url(targetUrl).head()
        headers.forEach { (k, v) -> reqBuilder.addHeader(k, v) }
        val request = reqBuilder.build()

        client.newCall(request).execute().use { response ->
            val rangeHeader = response.header("Accept-Ranges")?.lowercase()
            val currentEtag = response.header("ETag")
            val transferEncoding =
                response.header("Transfer-Encoding")?.lowercase()
            val isChunked = transferEncoding == "chunked"
            val contentLength =
                response.header("Content-Length")?.toLongOrNull()

            val effectiveSupportsRange =
                rangeHeader == "bytes" &&
                    contentLength != null &&
                    contentLength > 0

            if (isChunked) return ServerFileInfo(false, -1L, currentEtag)
            if (contentLength == null)
                throw IOException(
                    "Failed to get Content-Length and not chunked."
                )

            return ServerFileInfo(
                effectiveSupportsRange,
                contentLength,
                currentEtag
            )
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    private fun saveCheckpoint() {
        if (checkpoint == null) {
            println("Attempted to save a null checkpoint.")
            return
        }
        val tempMetaFile = File("$path.meta.tmp")
        try {
            metaFile.parentFile?.mkdirs()
            tempMetaFile.writeBytes(Cbor.encodeToByteArray(checkpoint!!))

            if (metaFile.exists()) {
                if (!metaFile.delete()) {
                    println(
                        "Warning: Could not delete old meta file: ${metaFile.path}. Attempting overwrite via copy."
                    )
                    try {
                        tempMetaFile.copyTo(metaFile, overwrite = true)
                        if (!tempMetaFile.delete()) {
                            println(
                                "Warning: Could not delete temp meta file after copy: ${tempMetaFile.path}"
                            )
                        }
                        println(
                            "Checkpoint saved successfully via copy (delete old failed)."
                        )
                        return
                    } catch (copyEx: Exception) {
                        println(
                            "Error saving checkpoint via copy fallback: ${copyEx.message}"
                        )
                        if (tempMetaFile.exists() && !tempMetaFile.delete()) {
                            println(
                                "Warning: Could not delete temp meta file after failed copy: ${tempMetaFile.path}"
                            )
                        }
                        return
                    }
                }
            }
            if (!tempMetaFile.renameTo(metaFile)) {
                tempMetaFile.copyTo(metaFile, overwrite = true)
                tempMetaFile.delete()
            }
        } catch (e: Exception) {
            println("Error saving checkpoint: ${e.message}")
            if (tempMetaFile.exists()) tempMetaFile.delete()
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    private fun loadCheckpoint(): DownloadCheckpoint? {
        if (!metaFile.exists()) return null
        return try {
            Cbor.decodeFromByteArray<DownloadCheckpoint>(metaFile.readBytes())
        } catch (e: Exception) {
            println(
                "Failed to load checkpoint: ${e.message}. Deleting corrupted meta file."
            )
            metaFile.delete()
            null
        }
    }

    // add remove download listener
    override fun addDownloadListener(listener: IDownloadListener) {
        downloadListeners.add(listener)
    }

    override fun removeDownloadListener(listener: IDownloadListener) {
        downloadListeners.remove(listener)
    }
}
