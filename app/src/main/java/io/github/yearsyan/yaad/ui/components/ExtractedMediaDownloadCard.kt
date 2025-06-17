package io.github.yearsyan.yaad.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.yaad.downloader_core.DownloadState
import io.github.yearsyan.yaad.R
import io.github.yearsyan.yaad.downloader.DownloadManager
import io.github.yearsyan.yaad.utils.FormatUtils
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardBottomSheet(
    onDismissRequest: () -> Unit,
    onRemove: () -> Unit = {},
    onOpenFile: () -> Unit = {}
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        ListItem(
            headlineContent = {
                Text(stringResource(R.string.popup_item_delete))
            },
            leadingContent = {
                Icon(Icons.Default.Delete, contentDescription = null)
            },
            modifier =
                Modifier.clickable(onClick = onRemove)
                    .background(MaterialTheme.colorScheme.surface)
        )
        ListItem(
            headlineContent = {
                Text(stringResource(R.string.popup_item_open_folder))
            },
            leadingContent = {
                Icon(Icons.Default.FileOpen, contentDescription = null)
            },
            modifier =
                Modifier.clickable(onClick = onRemove)
                    .background(MaterialTheme.colorScheme.surface)
        )
    }
}

@Composable
fun ExtractedMediaDownloadCard(
    scope: CoroutineScope,
    record: DownloadManager.ExtractedMediaDownloadSessionRecord,
    modifier: Modifier = Modifier,
    onRemove: (DownloadManager.ExtractedMediaDownloadSessionRecord) -> Unit =
        {},
    onOpenFile: (filePath: String) -> Unit = {}
) {
    // 获取实时状态
    val currentStatus by
        produceState(
            initialValue = getExtractedMediaStatus(record),
            key1 = record
        ) {
            while (true) {
                value = getExtractedMediaStatus(record)
                if (value == ExtractedMediaStatus.COMPLETED) {
                    break
                }
                delay(1000) // 每秒更新一次
            }
        }
    var showPopup by remember { mutableStateOf(false) }

    val totalMediaCount = record.mediaUrls.size
    val completedCount = record.completedCount
    // 获取当前下载速度
    val totalSpeed =
        record.childSessions.sumOf { childRecord ->
            childRecord.httpDownloadSession?.getStatus()?.speed?.toDouble()
                ?: 0.0
        }
    val speedKbps = totalSpeed / 1024

    if (showPopup) {
        CardBottomSheet(
            onDismissRequest = { showPopup = false },
            onRemove = {
                showPopup = false
                onRemove(record)
            },
            onOpenFile = { showPopup = false }
        )
    }

    Card(
        modifier = modifier.fillMaxWidth().clickable { showPopup = true },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
            // 标题显示原始链接
            Text(
                text = getDisplayName(record.title),
                style = MaterialTheme.typography.titleLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (currentStatus != ExtractedMediaStatus.COMPLETED) {
                Spacer(modifier = Modifier.height(8.dp))
                ExtractDownloadProgressBar(
                    modifier = Modifier.fillMaxWidth().height(12.dp),
                    record = record
                )
            }

            if (currentStatus == ExtractedMediaStatus.DOWNLOADING) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "已完成: $completedCount/$totalMediaCount",
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (speedKbps > 0) {
                        Text(
                            text = FormatUtils.formatSpeed(speedKbps),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 状态信息
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val statusVisuals =
                        getExtractedMediaStatusVisuals(currentStatus)
                    Icon(
                        imageVector = statusVisuals.first,
                        contentDescription =
                            getExtractedMediaStatusText(currentStatus),
                        modifier = Modifier.size(20.dp),
                        tint = statusVisuals.second
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = getExtractedMediaStatusText(currentStatus),
                        style = MaterialTheme.typography.bodyMedium,
                        color = statusVisuals.second,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1.0f))

            ExtractedMediaActionButtons(
                state = currentStatus,
                onPauseAll = {
                    scope.launch {
                        record.childSessions.forEach { childRecord ->
                            childRecord.httpDownloadSession?.pause()
                        }
                    }
                },
                onResumeAll = {
                    scope.launch {
                        record.childSessions.forEach { childRecord ->
                            childRecord.httpDownloadSession?.resume()
                        }
                    }
                },
                onStopAll = {
                    scope.launch {
                        record.childSessions.forEach { childRecord ->
                            childRecord.httpDownloadSession?.stop()
                        }
                    }
                },
                onRetryAll = {
                    scope.launch {
                        record.childSessions.forEach { childRecord ->
                            childRecord.httpDownloadSession?.start()
                        }
                    }
                },
                onRemove = { scope.launch { onRemove(record) } },
                onOpenFile = {
                    val file = File(record.savePath)
                    if (file.exists()) {
                        onOpenFile(file.absolutePath)
                    }
                }
            )
        }
    }
}

@Composable
private fun ExtractedMediaActionButtons(
    state: ExtractedMediaStatus,
    onPauseAll: () -> Unit,
    onResumeAll: () -> Unit,
    onStopAll: () -> Unit,
    onRetryAll: () -> Unit,
    onRemove: () -> Unit,
    onOpenFile: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        when (state) {
            ExtractedMediaStatus.DOWNLOADING -> {
                ExtractedMediaActionButton(
                    "全部暂停",
                    Icons.Filled.Pause,
                    onPauseAll,
                    isPrimary = false
                )
                Spacer(modifier = Modifier.width(8.dp))
                ExtractedMediaActionButton(
                    "全部停止",
                    Icons.Filled.Stop,
                    onStopAll,
                    isPrimary = false,
                    tint = MaterialTheme.colorScheme.error
                )
            }
            ExtractedMediaStatus.PAUSED -> {
                ExtractedMediaActionButton(
                    "全部继续",
                    Icons.Filled.PlayArrow,
                    onResumeAll,
                    isPrimary = true
                )
                Spacer(modifier = Modifier.width(8.dp))
                ExtractedMediaActionButton(
                    "全部停止",
                    Icons.Filled.Stop,
                    onStopAll,
                    isPrimary = false,
                    tint = MaterialTheme.colorScheme.error
                )
            }
            ExtractedMediaStatus.STOPPED -> {
                ExtractedMediaActionButton(
                    "全部重试",
                    Icons.Filled.Refresh,
                    onRetryAll,
                    isPrimary = true
                )
                Spacer(modifier = Modifier.width(8.dp))
                ExtractedMediaActionButton(
                    stringResource(R.string.button_remove),
                    Icons.Filled.DeleteOutline,
                    onRemove,
                    isPrimary = false,
                    tint = MaterialTheme.colorScheme.error
                )
            }
            ExtractedMediaStatus.COMPLETED -> {
                ExtractedMediaActionButton(
                    "打开",
                    Icons.Filled.FileOpen,
                    onOpenFile,
                    isPrimary = true
                )
            }
            ExtractedMediaStatus.ERROR -> {
                ExtractedMediaActionButton(
                    "全部重试",
                    Icons.Filled.Refresh,
                    onRetryAll,
                    isPrimary = true
                )
                Spacer(modifier = Modifier.width(8.dp))
                ExtractedMediaActionButton(
                    stringResource(R.string.button_remove),
                    Icons.Filled.DeleteOutline,
                    onRemove,
                    isPrimary = false,
                    tint = MaterialTheme.colorScheme.error
                )
            }
            ExtractedMediaStatus.PENDING -> {
                ExtractedMediaActionButton(
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
private fun ExtractedMediaActionButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isPrimary: Boolean = true,
    tint: Color = LocalContentColor.current
) {
    if (isPrimary) {
        Button(
            onClick = onClick,
            modifier = modifier.height(30.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
        ) {
            Icon(
                icon,
                contentDescription = text,
                Modifier.size(ButtonDefaults.IconSize)
            )
            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
            Text(text, style = MaterialTheme.typography.labelSmall)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier.height(30.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
        ) {
            Icon(
                icon,
                contentDescription = text,
                Modifier.size(ButtonDefaults.IconSize),
                tint = tint
            )
            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
            Text(
                text,
                color = tint,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

// 定义提取媒体下载状态枚举
enum class ExtractedMediaStatus {
    PENDING,
    DOWNLOADING,
    PAUSED,
    STOPPED,
    COMPLETED,
    ERROR
}

// 获取提取媒体下载的状态
private fun getExtractedMediaStatus(
    record: DownloadManager.ExtractedMediaDownloadSessionRecord
): ExtractedMediaStatus {

    if (record.downloadState == DownloadState.COMPLETED) {
        return ExtractedMediaStatus.COMPLETED
    }

    if (record.childSessions.isEmpty()) {
        return ExtractedMediaStatus.PENDING
    }

    val childStates =
        record.childSessions.mapNotNull {
            it.httpDownloadSession?.getStatus()?.state
        }

    return when {
        childStates.all { it == DownloadState.COMPLETED } ->
            ExtractedMediaStatus.COMPLETED
        childStates.any { it == DownloadState.ERROR } ->
            ExtractedMediaStatus.ERROR
        childStates.any {
            it == DownloadState.DOWNLOADING || it == DownloadState.PENDING
        } -> ExtractedMediaStatus.DOWNLOADING
        childStates.any { it == DownloadState.PAUSED } ->
            ExtractedMediaStatus.PAUSED
        childStates.all { it == DownloadState.STOPPED } ->
            ExtractedMediaStatus.STOPPED
        else -> ExtractedMediaStatus.PENDING
    }
}

// 获取状态文本
@Composable
private fun getExtractedMediaStatusText(status: ExtractedMediaStatus): String {
    return when (status) {
        ExtractedMediaStatus.PENDING -> stringResource(R.string.status_pending)
        ExtractedMediaStatus.DOWNLOADING -> "正在下载媒体"
        ExtractedMediaStatus.PAUSED -> stringResource(R.string.status_paused)
        ExtractedMediaStatus.STOPPED -> stringResource(R.string.status_stopped)
        ExtractedMediaStatus.COMPLETED ->
            stringResource(R.string.status_completed)
        ExtractedMediaStatus.ERROR -> "部分下载失败"
    }
}

// 获取状态图标和颜色
@Composable
private fun getExtractedMediaStatusVisuals(
    status: ExtractedMediaStatus
): Pair<ImageVector, Color> {
    return when (status) {
        ExtractedMediaStatus.PENDING ->
            Icons.Filled.HourglassEmpty to
                MaterialTheme.colorScheme.onSurfaceVariant
        ExtractedMediaStatus.DOWNLOADING ->
            Icons.Filled.Download to MaterialTheme.colorScheme.primary
        ExtractedMediaStatus.PAUSED ->
            Icons.Filled.Pause to MaterialTheme.colorScheme.onSurfaceVariant
        ExtractedMediaStatus.STOPPED ->
            Icons.Filled.Stop to MaterialTheme.colorScheme.onSurfaceVariant
        ExtractedMediaStatus.COMPLETED ->
            Icons.Filled.CheckCircleOutline to Color(0xFF4CAF50)
        ExtractedMediaStatus.ERROR ->
            Icons.Filled.ErrorOutline to MaterialTheme.colorScheme.error
    }
}

// 从URL获取显示名称
private fun getDisplayName(url: String): String {
    return try {
        val uri = java.net.URI(url)
        val host = uri.host ?: "未知来源"
        val path = uri.path?.takeIf { it.isNotEmpty() && it != "/" } ?: ""
        when {
            path.isNotEmpty() -> "$host$path"
            else -> host
        }
    } catch (e: Exception) {
        url.take(50) + if (url.length > 50) "..." else ""
    }
}
