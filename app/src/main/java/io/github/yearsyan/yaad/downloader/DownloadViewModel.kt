package io.github.yearsyan.yaad.downloader

import androidx.lifecycle.ViewModel
import io.github.yaad.downloader_core.IDownloadSession
import io.github.yearsyan.yaad.downloader.DownloadManager.DownloadSessionRecord
import kotlinx.coroutines.flow.StateFlow

class DownloadViewModel : ViewModel(), IDownloadTaskListener {
    val tasksUiState: StateFlow<List<DownloadSessionRecord>> =
        DownloadManager.tasksFlow

    init {
        DownloadManager.registerDownloadTaskListener(this)
    }

    override fun onCleared() {
        super.onCleared()
        DownloadManager.unregisterDownloadTaskListener(this)
    }

    override fun onCreateTask(
        session: IDownloadSession,
        sessionRecord: DownloadSessionRecord
    ) {}

    override fun onDeleteTask(
        session: IDownloadSession,
        sessionRecord: DownloadSessionRecord
    ) {}

    override fun updateState(
        session: IDownloadSession,
        sessionRecord: DownloadSessionRecord
    ) {}

    override fun progress(
        session: IDownloadSession,
        sessionRecord: DownloadSessionRecord
    ) {}
}
