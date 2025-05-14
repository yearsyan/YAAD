package io.github.yaad.downloader_core

interface IDownloadListener {
    fun onComplete(session: IDownloadSession) {}

    fun onPause(session: IDownloadSession) {}

    fun onResume(session: IDownloadSession, savePath: String) {}

    fun onError(session: IDownloadSession, reason: Exception) {}

    fun onProgress(session: IDownloadSession) {}
}
