package io.github.yearsyan.yaad.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PauseCircleOutline
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.yaad.downloader_core.DownloadState
import io.github.yaad.downloader_core.HttpDownloadStatus
import io.github.yaad.downloader_core.IDownloadListener
import io.github.yaad.downloader_core.IDownloadSession
import io.github.yearsyan.yaad.R
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun DownloadCard(
    scope: CoroutineScope,
    downloadSession: IDownloadSession,
    fileName: String,
    modifier: Modifier = Modifier,
    onRemove: (IDownloadSession) -> Unit = {},
    onOpenFile: (filePath: String) -> Unit = {}
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

            if (currentState != DownloadState.COMPLETED) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    MultiThreadProgressBar(
                        status = currentStatus as HttpDownloadStatus,
                        total = totalSize,
                        modifier = Modifier.weight(1f).height(12.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    val percentage =
                        if (totalSize > 0)
                            (downloadedBytes.toFloat() / totalSize) * 100
                        else 0f
                    Text(
                        text =
                            String.format(
                                Locale.getDefault(),
                                stringResource(
                                    R.string.download_percentage_format
                                ),
                                percentage
                            ),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

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

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val statusVisuals = getStatusVisuals(currentState)
                    Icon(
                        imageVector = statusVisuals.first,
                        contentDescription =
                            stringResource(
                                R.string.status_content_description_format,
                                getStatusText(
                                    currentState,
                                    currentStatus.errorMessage
                                )
                            ),
                        modifier = Modifier.size(20.dp),
                        tint = statusVisuals.second
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text =
                            getStatusText(
                                currentState,
                                currentStatus.errorMessage
                            ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = statusVisuals.second,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                when (currentState) {
                    DownloadState.COMPLETED,
                    DownloadState.VALIDATING -> {
                        Text(
                            text = formatBytes(totalSize),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    DownloadState.DOWNLOADING -> {
                        val etr =
                            calculateEtr(
                                totalSize,
                                downloadedBytes,
                                speedKbps.toFloat()
                            )
                        Text(
                            text =
                                stringResource(
                                    R.string.download_etr_format,
                                    etr
                                ),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    else -> {
                        Row {}
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            DownloadActionButtons(
                state = currentState,
                onStart = { scope.launch { downloadSession.start() } },
                onPause = { scope.launch { downloadSession.pause() } },
                onResume = { scope.launch { downloadSession.resume() } },
                onStop = { scope.launch { downloadSession.stop() } },
                onRetry = { scope.launch { downloadSession.start() } },
                onRemove = { scope.launch { onRemove(downloadSession) } },
                onOpenFile = { /* downloadSession.getFilePath()?.let { onOpenFile(it) } */}
            )
        }
    }
}

@Composable
private fun DownloadActionButtons(
    state: DownloadState,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
    onRetry: () -> Unit,
    onRemove: () -> Unit,
    onOpenFile: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        when (state) {
            DownloadState.PENDING,
            DownloadState.DOWNLOADING -> {
                ActionButton(
                    stringResource(R.string.button_pause),
                    Icons.Filled.Pause,
                    onPause,
                    isPrimary = false
                )
                Spacer(modifier = Modifier.width(8.dp))
                ActionButton(
                    stringResource(R.string.button_stop),
                    Icons.Filled.Stop,
                    onStop,
                    isPrimary = false,
                    tint = MaterialTheme.colorScheme.error
                )
            }
            DownloadState.PAUSED -> {
                ActionButton(
                    stringResource(R.string.button_resume),
                    Icons.Filled.PlayArrow,
                    onResume,
                    isPrimary = true
                )
                Spacer(modifier = Modifier.width(8.dp))
                ActionButton(
                    stringResource(R.string.button_stop),
                    Icons.Filled.Stop,
                    onStop,
                    isPrimary = false,
                    tint = MaterialTheme.colorScheme.error
                )
            }
            DownloadState.STOPPED -> {
                ActionButton(
                    stringResource(R.string.button_start),
                    Icons.Filled.PlayArrow,
                    onStart,
                    isPrimary = true
                )
                Spacer(modifier = Modifier.width(8.dp))
                ActionButton(
                    stringResource(R.string.button_remove),
                    Icons.Filled.DeleteOutline,
                    onRemove,
                    isPrimary = false,
                    tint = MaterialTheme.colorScheme.error
                )
            }
            DownloadState.COMPLETED -> {
                ActionButton(
                    stringResource(R.string.button_open),
                    Icons.Filled.FileOpen,
                    onOpenFile,
                    isPrimary = true
                )
                Spacer(modifier = Modifier.width(8.dp))
                ActionButton(
                    stringResource(R.string.button_clear),
                    Icons.Filled.Clear,
                    onRemove,
                    isPrimary = false
                )
            }
            DownloadState.ERROR -> {
                ActionButton(
                    stringResource(R.string.button_retry),
                    Icons.Filled.Refresh,
                    onRetry,
                    isPrimary = true
                )
                Spacer(modifier = Modifier.width(8.dp))
                ActionButton(
                    stringResource(R.string.button_remove),
                    Icons.Filled.DeleteOutline,
                    onRemove,
                    isPrimary = false,
                    tint = MaterialTheme.colorScheme.error
                )
            }
            DownloadState.VALIDATING -> {
                ActionButton(
                    stringResource(R.string.button_stop),
                    Icons.Filled.Stop,
                    onStop,
                    isPrimary = false,
                    tint = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.width(8.dp))
                ActionButton(
                    stringResource(R.string.button_remove),
                    Icons.Filled.DeleteOutline,
                    onRemove,
                    isPrimary = false,
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun ActionButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isPrimary: Boolean = true,
    tint: Color = LocalContentColor.current
) {
    if (isPrimary) {
        Button(onClick = onClick, modifier = modifier) {
            Icon(
                icon,
                contentDescription = text,
                Modifier.size(ButtonDefaults.IconSize)
            )
            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
            Text(text)
        }
    } else {
        OutlinedButton(onClick = onClick, modifier = modifier) {
            Icon(
                icon,
                contentDescription = text,
                Modifier.size(ButtonDefaults.IconSize),
                tint = tint
            )
            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
            Text(text, color = tint)
        }
    }
}

@Composable
fun formatBytes(bytes: Long): String {
    if (bytes < 0) return stringResource(R.string.bytes_zero)
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    var i = 0
    var value = bytes.toDouble()
    while (value >= 1024 && i < units.size - 1) {
        value /= 1024.0
        i++
    }
    return String.format(Locale.getDefault(), "%.2f %s", value, units[i])
}

@Composable
fun formatSpeed(speedKbps: Double): String {
    if (speedKbps < 0) return stringResource(R.string.speed_zero)
    var value = speedKbps * 1024.0
    val units = arrayOf("B/s", "KB/s", "MB/s", "GB/s", "TB/s")
    var i = 0
    while (value >= 1024 && i < units.size - 1) {
        value /= 1024.0
        i++
    }
    return String.format(Locale.getDefault(), "%.2f %s", value, units[i])
}

@Composable
fun calculateEtr(
    totalBytes: Long,
    downloadedBytes: Long,
    speedKbps: Float
): String {
    if (speedKbps <= 0 || downloadedBytes >= totalBytes || totalBytes <= 0) {
        return ""
    }

    val speedBps = speedKbps * 1024 // B/s
    if (speedBps <= 0) return ""

    val remainingBytes = totalBytes - downloadedBytes
    val remainingSeconds = (remainingBytes / speedBps).toLong()

    if (remainingSeconds < 0) return "" // Should be unreachable
    val hours = remainingSeconds / 3600
    val minutes = (remainingSeconds % 3600) / 60
    val seconds = remainingSeconds % 60

    val strHoursSuffix = stringResource(R.string.etr_hours_suffix)
    val strMinutesSuffix = stringResource(R.string.etr_minutes_suffix)
    val strSecondsSuffix = stringResource(R.string.etr_seconds_suffix)
    val strLessThanOneSecond = stringResource(R.string.etr_less_than_one_second)

    return buildString {
            if (hours > 0) append("$hours$strHoursSuffix ")
            if (minutes > 0 || hours > 0) append("$minutes$strMinutesSuffix ")
            append("$seconds$strSecondsSuffix")
        }
        .trim()
        .ifEmpty { strLessThanOneSecond }
}

@Composable
fun getStatusText(state: DownloadState, errorMessage: String?): String {
    return when (state) {
        DownloadState.PENDING -> stringResource(R.string.status_pending)
        DownloadState.DOWNLOADING -> stringResource(R.string.status_downloading)
        DownloadState.PAUSED -> stringResource(R.string.status_paused)
        DownloadState.STOPPED -> stringResource(R.string.status_stopped)
        DownloadState.COMPLETED -> stringResource(R.string.status_completed)
        DownloadState.ERROR ->
            stringResource(
                R.string.status_error_format,
                errorMessage ?: stringResource(R.string.status_error_unknown)
            )
        DownloadState.VALIDATING -> stringResource(R.string.status_validating)
    }
}

@Composable
fun getStatusVisuals(state: DownloadState): Pair<ImageVector, Color> {
    return when (state) {
        DownloadState.PENDING ->
            Icons.Filled.HourglassEmpty to
                MaterialTheme.colorScheme.onSurfaceVariant
        DownloadState.DOWNLOADING ->
            Icons.Filled.Download to MaterialTheme.colorScheme.primary
        DownloadState.PAUSED ->
            Icons.Filled.PauseCircleOutline to
                MaterialTheme.colorScheme.onSurfaceVariant
        DownloadState.STOPPED ->
            Icons.Filled.RemoveCircleOutline to
                MaterialTheme.colorScheme.onSurfaceVariant
        DownloadState.COMPLETED ->
            Icons.Filled.CheckCircleOutline to Color(0xFF4CAF50)
        DownloadState.ERROR ->
            Icons.Filled.ErrorOutline to MaterialTheme.colorScheme.error
        DownloadState.VALIDATING ->
            Icons.Filled.Sync to MaterialTheme.colorScheme.primary
    }
}
