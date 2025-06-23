package io.github.yearsyan.yaad.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.yaad.downloader_core.DownloadState
import io.github.yaad.downloader_core.IDownloadListener
import io.github.yaad.downloader_core.IDownloadSession
import io.github.yearsyan.yaad.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun TorrentDownloadCard(
    scope: CoroutineScope,
    downloadSession: IDownloadSession,
    fileName: String,
    modifier: Modifier = Modifier,
) {
    val currentStatus by
        produceState(
            initialValue = downloadSession.getStatus(),
            key1 = downloadSession
        ) {
            value = withContext(Dispatchers.IO) { downloadSession.getStatus() }

            val listener =
                object : IDownloadListener {
                    override fun onProgress(session: IDownloadSession) {
                        val status = downloadSession.getStatus()
                        this@produceState.launch(Dispatchers.Main) {
                            value = status
                        }
                    }
                }
            downloadSession.addDownloadListener(listener)

            awaitDispose { downloadSession.removeDownloadListener(listener) }
        }

    val totalSize = downloadSession.getStatus().totalSize
    val downloadedBytes = currentStatus.totalDownloaded
    val speedKbps = currentStatus.downloadSpeed / 1024
    val currentState = currentStatus.state

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
            Text(
                text = fileName,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (
                currentState == DownloadState.DOWNLOADING ||
                    currentState == DownloadState.PAUSED
            ) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text =
                            stringResource(
                                R.string.download_size_progress,
                                formatBytes(downloadedBytes),
                                formatBytes(totalSize)
                            ),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text =
                            stringResource(
                                R.string.download_speed_format,
                                formatSpeed(speedKbps)
                            ),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}
