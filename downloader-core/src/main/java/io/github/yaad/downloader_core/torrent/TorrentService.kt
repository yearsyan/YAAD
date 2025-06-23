package io.github.yaad.downloader_core.torrent

import androidx.annotation.Keep
import io.github.yaad.downloader_core.BaseDownloadStatus
import io.github.yaad.downloader_core.DownloadState
import java.lang.ref.WeakReference

class TorrentDownloadStatus(
    percent: Int,
    totalDownloaded: Long,
    downloadSpeed: Double,
    val uploadSpeed: Double,
    state: DownloadState,
    totalSize: Long,
    errorMessage: String? = ""
) :
    BaseDownloadStatus(
        percent,
        totalDownloaded,
        downloadSpeed,
        state,
        totalSize,
        errorMessage
    )

class TorrentService {

    private val sessionMap:
        HashMap<Long, WeakReference<TorrentDownloadSession>> =
        HashMap()

    init {
        System.loadLibrary("downloader-core")
    }

    companion object {
        private var instance_: TorrentService? = null
        val instance
            get() = {
                if (instance_ == null) {
                    instance_ = TorrentService()
                }
                instance_!!
            }

        @Keep
        @JvmStatic
        fun createDownloadStatus(
            percent: Int,
            totalDownloaded: Long,
            downloadSpeed: Double,
            uploadSpeed: Double,
            totalSize: Long
        ): TorrentDownloadStatus {
            return TorrentDownloadStatus(
                percent = percent,
                totalDownloaded = totalDownloaded,
                downloadSpeed = downloadSpeed,
                uploadSpeed = uploadSpeed,
                state = DownloadState.DOWNLOADING,
                totalSize = totalSize
            )
        }
    }

    @Keep private var ptr: Long = 0
    private var queryThread: Thread
    private var lastUpdateTime: Long = 0
    private var updateDelta = 500L

    constructor() {
        initService()
        queryThread = Thread {
            while (true) {
                val delta =
                    updateDelta - (System.currentTimeMillis() - lastUpdateTime)
                if (delta > 0) {
                    Thread.sleep(delta)
                }
                torrentUpdate()
                lastUpdateTime = System.currentTimeMillis()
            }
        }
        queryThread.start()
    }

    @Keep
    fun onTaskUpdate(id: Long, st: TorrentDownloadStatus) {
        val session = sessionMap[id]?.get()
        session?.onStatusUpdate(st)
    }

    external fun initService()

    external fun addTaskByLink(link: String, save: String): Long

    external fun getTaskStatus(id: Long): TorrentDownloadStatus

    external fun taskPause(id: Long)

    external fun taskResume(id: Long)

    external fun taskRemove(id: Long)

    external fun torrentUpdate()
}
