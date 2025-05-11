package io.github.yaad.downloader_core

interface IDownloadSession {
    val total: Long
    fun getStatus(): DownloadStatus
    fun pause()
    fun resume()
    fun stop()
    fun start()
    fun remove()
}