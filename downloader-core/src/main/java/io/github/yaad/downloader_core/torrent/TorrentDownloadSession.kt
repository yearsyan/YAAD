package io.github.yaad.downloader_core.torrent

import androidx.annotation.Keep
import io.github.yaad.downloader_core.DownloadState
import io.github.yaad.downloader_core.IDownloadListener
import io.github.yaad.downloader_core.IDownloadSession

enum class SourceType {
    Torrent,
    Magnet
}

class TorrentDownloadSession : IDownloadSession {

    private var taskId: Long = -1

    private val torrentService = TorrentService.instance()
    private var sourceType: SourceType = SourceType.Torrent
    private var sourceInfo: String = ""
    private val savePath: String
    private val downloadListeners: HashSet<IDownloadListener> = HashSet()
    private var status: TorrentDownloadStatus? = null

    private constructor(
        sourceType: SourceType,
        sourceInfo: String,
        savePath: String
    ) {
        this.sourceType = sourceType
        this.sourceInfo = sourceInfo
        this.savePath = savePath
    }

    companion object {
        @Keep
        @JvmStatic
        fun createByLink(
            link: String,
            savePath: String
        ): TorrentDownloadSession {
            val session =
                TorrentDownloadSession(
                    sourceType = SourceType.Torrent,
                    sourceInfo = link,
                    savePath = savePath
                )
            return session
        }
    }

    fun onStatusUpdate(s: TorrentDownloadStatus) {
        for (l in downloadListeners) {
            l.onProgress(this)
        }
        status = s
    }

    override fun getStatus(): TorrentDownloadStatus {
        val st = status
        if (st == null) {
            return TorrentDownloadStatus(
                percent = 0,
                totalDownloaded = 0,
                downloadSpeed = 0.0,
                uploadSpeed = 0.0,
                state = DownloadState.PENDING,
                totalSize = 0,
            )
        }
        return st
    }

    override suspend fun start(
        starResultListener: (Exception?) -> Unit,
        finishListener: () -> Unit
    ) {
        if (sourceType == SourceType.Magnet) {
            taskId = torrentService.addTaskByLink(sourceInfo, savePath)
        }
    }

    override suspend fun pause() {
        torrentService.taskPause(taskId)
    }

    override suspend fun resume() {
        torrentService.taskResume(taskId)
    }

    override suspend fun stop() {
        torrentService.taskRemove(taskId)
    }

    override suspend fun remove() {
        torrentService.taskRemove(taskId)
    }

    override fun addDownloadListener(listener: IDownloadListener) {
        downloadListeners.add(listener)
    }

    override fun removeDownloadListener(listener: IDownloadListener) {
        downloadListeners.remove(listener)
    }
}
