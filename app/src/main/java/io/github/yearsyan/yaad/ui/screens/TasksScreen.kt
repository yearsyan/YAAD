package io.github.yearsyan.yaad.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.yearsyan.yaad.downloader.DownloadManager
import io.github.yearsyan.yaad.downloader.DownloadViewModel
import io.github.yearsyan.yaad.ui.components.DownloadCard
import kotlinx.coroutines.CoroutineScope

@Composable
fun TasksScreen(scope: CoroutineScope, viewModel: DownloadViewModel) {
    val tasks by viewModel.tasksUiState.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(tasks, key = { it.sessionId }) { record ->
            DownloadItem(scope, record)
        }
    }
}

@Composable
fun DownloadItem(scope: CoroutineScope, record: DownloadManager.DownloadSessionRecord) {
    if (record is DownloadManager.SingleHttpDownloadSessionRecord && record.httpDownloadSession != null) {
        val session = record.httpDownloadSession!!
        DownloadCard(
            scope = scope,
            downloadSession = session,
            fileName = session.fileName
        )
    } else {
        Box {  }
    }
}
