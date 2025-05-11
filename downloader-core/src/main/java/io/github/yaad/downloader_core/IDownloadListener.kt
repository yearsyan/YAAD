package io.github.yaad.downloader_core

interface IDownloadListener {
    fun onComplete() {}

    fun onPause() {}

    fun onResume(savePath: String) {}

    fun onError(reason: Exception) {}

    fun onProgress() {}
}
