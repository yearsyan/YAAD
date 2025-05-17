package io.github.yearsyan.yaad.downloader

import io.github.yaad.downloader_core.IDownloadSession

interface IDownloadTaskListener {
    fun updateState(session: IDownloadSession, sessionRecord: DownloadManager.DownloadSessionRecord)
    fun progress(session: IDownloadSession, sessionRecord: DownloadManager.DownloadSessionRecord)
    fun onCreateTask(session: IDownloadSession, sessionRecord: DownloadManager.DownloadSessionRecord)
    fun onDeleteTask(session: IDownloadSession, sessionRecord: DownloadManager.DownloadSessionRecord)
}
