package io.github.yearsyan.yaad.ui.screens

import android.content.Intent
import android.os.Build
import android.provider.DocumentsContract
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import io.github.yearsyan.yaad.downloader.DownloadManager
import io.github.yearsyan.yaad.downloader.DownloadViewModel
import io.github.yearsyan.yaad.ui.components.DownloadCard
import io.github.yearsyan.yaad.ui.components.ExtractedMediaDownloadCard
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun TasksScreen(scope: CoroutineScope, viewModel: DownloadViewModel) {
    val tasks by viewModel.tasksUiState.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(tasks, key = { it.sessionId }) { record ->
            DownloadItem(scope, record)
        }
    }
}

@Composable
fun DownloadItem(
    scope: CoroutineScope,
    record: DownloadManager.DownloadSessionRecord
) {
    val context = LocalContext.current

    if (
        record is DownloadManager.SingleHttpDownloadSessionRecord &&
            record.httpDownloadSession != null
    ) {
        val session = record.httpDownloadSession!!
        DownloadCard(
            scope = scope,
            downloadSession = session,
            fileName = session.fileName
        )
    } else if (record is DownloadManager.ExtractedMediaDownloadSessionRecord) {
        ExtractedMediaDownloadCard(
            scope = scope,
            record = record,
            onRemove = { recordToRemove ->
                scope.launch {
                    DownloadManager.deleteDownloadTask(recordToRemove.sessionId)
                }
            },
            onOpenFolder = { folderPath ->
                val intent =
                    Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
                        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            putExtra(
                                DocumentsContract.EXTRA_INITIAL_URI,
                                folderPath.toUri()
                            )
                        }
                    }
                context.startActivity(intent)
            }
        )
    }
}
