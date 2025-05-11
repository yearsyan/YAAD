package io.github.yaad.downloader_core

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.decodeFromByteArray
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

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
    val parts: List<ThreadPartInfo>
)

enum class DownloadState {
    PENDING,
    DOWNLOADING,
    PAUSED,
    STOPPED,
    COMPLETED,
    ERROR
}

data class DownloadStatus(
    val percent: Int,
    val totalDownloaded: Long,
    val parts: List<ThreadPartInfo>,
    val speed: Double,
    val state: DownloadState,
    val errorMessage: String? = ""
)

class HttpDownloadSession(
    private val url: String,
    private val path: String,
    private val headers: Map<String, String> = emptyMap(),
    private val threadCount: Int = 8
) : IDownloadSession {
    companion object {
        private val client = OkHttpClient()
    }

    private val metaFile = File("$path.meta")
    private var checkpoint: DownloadCheckpoint? = null
    private var fd: Int = -1
    private var ptr: Long = 0L
    private var supportsRange = false
    private val speedUpdateTime: Long = 200
    private var totalFileSize: Long = 0
    override val total: Long
        get() = totalFileSize

    @Volatile private var isPaused = false
    @Volatile private var isStopped = false
    @Volatile private var currentState = DownloadState.DOWNLOADING
    private val lock = Object()

    override fun start() {
        val file = File(path)
        if (!file.exists()) file.createNewFile()

        val (rangeSupported, fileSize) = checkSupportForRange(url)
        totalFileSize = fileSize
        supportsRange = rangeSupported

        // fallback for chunked (no Content-Length, no Range)
        if (!supportsRange && fileSize == -1L) {
            performChunkedDownload()
            return
        }

        fd = NativeBridge.openFile(path)
        ptr = NativeBridge.mmapFile(fd, fileSize)

        checkpoint = if (supportsRange && metaFile.exists()) {
            loadCheckpoint()
        } else {
            val parts = if (supportsRange) {
                val partSize = fileSize / threadCount
                List(threadCount) { i ->
                    val start = i * partSize
                    val end = if (i == threadCount - 1) fileSize - 1 else (start + partSize - 1)
                    ThreadPartInfo(start, end, 0L)
                }
            } else {
                listOf(ThreadPartInfo(0, fileSize - 1, 0L))
            }
            DownloadCheckpoint(url, fileSize, parts)
        }

        val executor = Executors.newFixedThreadPool(checkpoint!!.parts.size)
        currentState = DownloadState.DOWNLOADING

        val progressPrinter = Thread {
            while (!executor.isTerminated) {
                if (supportsRange) saveCheckpoint()
                Thread.sleep(1000)
            }
        }

        try {
            progressPrinter.start()

            checkpoint!!.parts.forEachIndexed { _, part ->
                executor.submit {
                    val startOffset = part.start + part.downloaded
                    if (startOffset > part.end) return@submit

                    val reqBuilder = Request.Builder().url(url)
                    if (supportsRange) {
                        reqBuilder.addHeader("Range", "bytes=$startOffset-${part.end}")
                    }
                    headers.forEach { (k, v) -> reqBuilder.addHeader(k, v) }

                    val req = reqBuilder.build()

                    client.newCall(req).execute().use { response ->
                        if (!response.isSuccessful) throw RuntimeException("HTTP error: ${response.code}")
                        val body = response.body ?: throw RuntimeException("No response body")
                        val inputStream = body.byteStream()
                        val buffer = ByteArray(65536)
                        var offset = startOffset

                        while (true) {
                            synchronized(lock) {
                                while (isPaused) lock.wait()
                                if (isStopped) return@submit
                            }

                            val read = inputStream.read(buffer)
                            if (read == -1) break

                            for (j in 0 until read) {
                                NativeBridge.writeByte(ptr, offset + j, buffer[j])
                            }
                            offset += read
                            part.downloaded += read
                        }
                    }
                }
            }

            executor.shutdown()
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS)

            NativeBridge.msync(ptr, checkpoint!!.fileSize)
            NativeBridge.resizeFile(fd, checkpoint!!.fileSize)
            NativeBridge.munmap(ptr, checkpoint!!.fileSize)
            NativeBridge.closeFile(fd)

            if (supportsRange) metaFile.delete()
            progressPrinter.join()

            currentState = DownloadState.COMPLETED

        } catch (e: Exception) {
            NativeBridge.munmap(ptr, checkpoint!!.fileSize)
            NativeBridge.closeFile(fd)
            if (supportsRange) saveCheckpoint()
            currentState = DownloadState.ERROR
            throw e
        }
    }

    private fun performChunkedDownload() {
        currentState = DownloadState.DOWNLOADING

        try {
            val request = Request.Builder().url(url).apply {
                headers.forEach { (k, v) -> addHeader(k, v) }
            }.build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw RuntimeException("HTTP error: ${response.code}")

                val input = response.body?.byteStream() ?: throw RuntimeException("No response body")
                val outputFile = File(path)

                FileOutputStream(outputFile).use { output ->
                    val buffer = ByteArray(65536)

                    while (true) {
                        synchronized(lock) {
                            while (isPaused) lock.wait()
                        }
                        if (isStopped) return

                        val read = input.read(buffer)
                        if (read == -1) break
                        output.write(buffer, 0, read)
                    }
                }
            }

            currentState = if (isStopped) DownloadState.STOPPED else DownloadState.COMPLETED

        } catch (e: Exception) {
            currentState = DownloadState.ERROR
            throw e
        }
    }

    private fun updateThreadSpeed() {
        val parts = checkpoint?.parts ?: return
        parts.forEach {
            val current = System.currentTimeMillis()
            val downloaded = it.downloaded
            if (current - it.lastKeyTime > speedUpdateTime) {
                val deltaDownload = it.downloaded - it.lastKeyDownLoad
                val deltaTime = current - it.lastKeyTime
                it.speed = deltaDownload / (deltaTime / 1000.0)
                it.lastKeyTime = current
                it.lastKeyDownLoad = downloaded
            }
        }
    }

    @Synchronized
    override fun getStatus(): DownloadStatus {
        updateThreadSpeed()
        val c = checkpoint
        val downloaded = c?.parts?.sumOf { it.downloaded } ?: File(path).length()
        val percent = if (totalFileSize <= 0) 0 else (downloaded * 100 / totalFileSize).toInt()
        val speed = c?.parts?.sumOf { it.speed } ?: 0.0
        val parts = c?.parts?.map { it.copy() } ?: emptyList()
        return DownloadStatus(percent, downloaded, parts, speed, currentState)
    }

    override fun pause() {
        isPaused = true
        currentState = DownloadState.PAUSED
        if (supportsRange) saveCheckpoint()
    }

    override fun resume() {
        synchronized(lock) {
            isPaused = false
            lock.notifyAll()
        }
        currentState = DownloadState.DOWNLOADING
    }

    override fun stop() {
        isStopped = true
        currentState = DownloadState.STOPPED
        synchronized(lock) {
            lock.notifyAll()
        }
        if (supportsRange) saveCheckpoint()
    }

    override fun remove() {
        stop()
        File(path).delete()
        metaFile.delete()
        currentState = DownloadState.STOPPED
    }

    private fun checkSupportForRange(url: String): Pair<Boolean, Long> {
        val reqBuilder = Request.Builder().url(url).head()
        headers.forEach { (k, v) -> reqBuilder.addHeader(k, v) }
        val request = reqBuilder.build()

        client.newCall(request).execute().use { response ->
            val supportsRange = response.header("Accept-Ranges")?.lowercase() == "bytes"
            val transferEncoding = response.header("Transfer-Encoding")?.lowercase()
            val isChunked = transferEncoding == "chunked"
            val contentLength = response.header("Content-Length")?.toLongOrNull()

            if (isChunked) return Pair(false, -1L)
            if (contentLength == null) throw RuntimeException("Failed to get Content-Length")

            return Pair(supportsRange, contentLength)
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    private fun saveCheckpoint() {
        metaFile.writeBytes(Cbor.encodeToByteArray(checkpoint))
    }

    @OptIn(ExperimentalSerializationApi::class)
    private fun loadCheckpoint(): DownloadCheckpoint {
        return Cbor.decodeFromByteArray(metaFile.readBytes())
    }
}
