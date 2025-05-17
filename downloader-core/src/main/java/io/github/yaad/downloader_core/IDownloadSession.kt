package io.github.yaad.downloader_core

interface IDownloadSession {
    val total: Long

    fun getStatus(): DownloadStatus

    suspend fun start(starResultListener: (e: Exception?) -> Unit = {})

    suspend fun pause()

    suspend fun resume()

    suspend fun stop()

    suspend fun remove()

    fun addDownloadListener(listener: IDownloadListener)

    fun removeDownloadListener(listener: IDownloadListener)
}
