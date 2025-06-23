package io.github.yaad.downloader_core

open class BaseDownloadStatus(
    val percent: Int,
    val totalDownloaded: Long,
    val downloadSpeed: Double,
    val state: DownloadState,
    val totalSize: Long,
    val errorMessage: String? = ""
)

interface IDownloadSession {

    fun getStatus(): BaseDownloadStatus

    suspend fun start(
        starResultListener: (e: Exception?) -> Unit = {},
        finishListener: () -> Unit = {}
    )

    suspend fun pause()

    suspend fun resume()

    suspend fun stop()

    suspend fun remove()

    fun addDownloadListener(listener: IDownloadListener)

    fun removeDownloadListener(listener: IDownloadListener)
}
