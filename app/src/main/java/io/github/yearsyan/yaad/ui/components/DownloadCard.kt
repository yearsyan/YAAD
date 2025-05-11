package io.github.yearsyan.yaad.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
// 在实际项目中，推荐使用 stringResource 来管理所有面向用户的文本
// import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.yaad.downloader_core.DownloadState
import io.github.yaad.downloader_core.DownloadStatus // 假设 DownloadStatus 是您已有的一个类
import io.github.yaad.downloader_core.IDownloadSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.util.Locale


@Composable
fun DownloadCard(
    downloadSession: IDownloadSession, // 下载会话接口
    fileName: String, // 文件名，对于用户了解下载内容至关重要
    modifier: Modifier = Modifier,
    onRemove: (IDownloadSession) -> Unit = {}, // 移除/清除下载项的回调
    onOpenFile: (filePath: String) -> Unit = {} // 打开已下载文件的回调
) {
    // 使用 `produceState` 来更清晰地处理来自流式数据源的状态更新
    // 当 Composable 离开组合时，它也会自动处理取消操作。
    // `key1 = downloadSession` 确保如果 downloadSession 实例发生变化，生产者会重启。
    val currentStatus by produceState(initialValue = downloadSession.getStatus(), key1 = downloadSession) {
        while (isActive) { // `isActive` 来自 produceState 的协程作用域
            value = withContext(Dispatchers.IO) { // 在非主线程执行可能阻塞的 getStatus
                downloadSession.getStatus()
            }
            delay(500) // 更新频率
        }
    }

    val totalSize = downloadSession.total // 总大小
    val downloadedBytes = currentStatus.totalDownloaded // 已下载大小
    val speedKbps = currentStatus.speed / 1024 // 下载速度
    val currentState = currentStatus.state // 当前状态

    Card(
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 8.dp) // 卡片外边距
            .fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp), // 卡片阴影
        shape = MaterialTheme.shapes.medium // 卡片形状
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp) // 内容区域内边距
                .fillMaxWidth()
        ) {
            // 文件名 (更突出)
            Text(
                text = fileName, // 使用传入的文件名
                style = MaterialTheme.typography.titleLarge, // 标题使用大号字体
                maxLines = 2, // 允许文件名最多显示两行
                overflow = TextOverflow.Ellipsis // 超出部分显示省略号
            )
            Spacer(modifier = Modifier.height(8.dp)) // 垂直间距

            // 进度条和百分比文本
            Row(
                verticalAlignment = Alignment.CenterVertically, // 垂直居中对齐
                modifier = Modifier.fillMaxWidth()
            ) {
                MultiThreadProgressBar(
                    status = currentStatus,
                    total = totalSize,
                    modifier = Modifier
                        .weight(1f) // 占据可用空间
                        .height(12.dp) // 进度条高度
                )
                Spacer(modifier = Modifier.width(12.dp)) // 水平间距
                val percentage = if (totalSize > 0) (downloadedBytes.toFloat() / totalSize) * 100 else 0f
                Text(
                    text = String.format(Locale.getDefault(), "%.1f%%", percentage), // 格式化百分比，保留一位小数
                    style = MaterialTheme.typography.bodyMedium // 正文文本样式
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            // 下载信息：大小、速度
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween // 子元素水平均匀分布
            ) {
                Text(
                    // text = stringResource(R.string.download_size_progress, formatBytes(downloadedBytes), formatBytes(totalSize)), // 实际项目中应使用字符串资源
                    text = "${formatBytes(downloadedBytes)} / ${formatBytes(totalSize)}", // 显示已下载/总大小
                    style = MaterialTheme.typography.bodySmall // 小号正文文本样式
                )
                Text(
                    // text = stringResource(R.string.download_speed, "%.2f".format(speedKbps)), // 实际项目中应使用字符串资源
                    text = "速度: ${formatSpeed(speedKbps)}", // 显示下载速度
                    style = MaterialTheme.typography.bodySmall
                )
            }

            // 预计剩余时间 (ETR)
            val etr = calculateEtr(totalSize, downloadedBytes, speedKbps.toFloat())
            if (etr.isNotBlank() && currentState == DownloadState.DOWNLOADING) {
                Text(
                    // text = stringResource(R.string.download_etr, etr), // 实际项目中应使用字符串资源
                    text = "剩余时间: $etr",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.align(Alignment.End) // ETR 文本右对齐
                )
            }
            Spacer(modifier = Modifier.height(4.dp))

            // 状态文本及图标
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                val statusVisuals = getStatusVisuals(currentState) // 获取状态对应的图标和颜色
                Icon(
                    imageVector = statusVisuals.first, // 状态图标
                    contentDescription = "状态: ${getStatusText(currentState, currentStatus.errorMessage)}", // 内容描述，用于辅助功能
                    modifier = Modifier.size(20.dp), // 图标大小
                    tint = statusVisuals.second // 图标颜色
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = getStatusText(currentState, currentStatus.errorMessage), // 显示本地化的状态文本，包含错误信息
                    style = MaterialTheme.typography.bodyMedium,
                    color = statusVisuals.second, // 文本颜色与图标一致
                    maxLines = 2, // 允许状态文本（尤其是错误信息）最多显示两行
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 操作按钮 - 根据上下文显示，更清晰
            DownloadActionButtons(
                state = currentState,
                onStart = { downloadSession.start() },
                onPause = { downloadSession.pause() },
                onResume = { downloadSession.resume() },
                onStop = { downloadSession.stop() },
                // 重试操作通常等同于开始，如果会话支持特定重试逻辑则可另行处理
                onRetry = { downloadSession.start() },
                onRemove = { onRemove(downloadSession) },
                onOpenFile = {
                    // 假设 IDownloadSession 中有 getFilePath() 方法
                    // 并且在下载完成后会返回非空路径
                    // downloadSession.getFilePath()?.let { onOpenFile(it) }
                }
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
        horizontalArrangement = Arrangement.End // 按钮组靠右对齐
    ) {
        when (state) {
            DownloadState.PENDING, DownloadState.DOWNLOADING -> {
                ActionButton(
                    text = "暂停", // stringResource(R.string.button_pause),
                    icon = Icons.Filled.Pause,
                    onClick = onPause,
                    isPrimary = false // 次要按钮
                )
                Spacer(modifier = Modifier.width(8.dp))
                ActionButton(
                    text = "停止", // stringResource(R.string.button_stop),
                    icon = Icons.Filled.Stop,
                    onClick = onStop,
                    isPrimary = false,
                    tint = MaterialTheme.colorScheme.error // 停止按钮通常用警示色
                )
            }
            DownloadState.PAUSED -> {
                ActionButton(
                    text = "继续", // stringResource(R.string.button_resume),
                    icon = Icons.Filled.PlayArrow,
                    onClick = onResume,
                    isPrimary = true // 主要按钮
                )
                Spacer(modifier = Modifier.width(8.dp))
                ActionButton(
                    text = "停止", // stringResource(R.string.button_stop),
                    icon = Icons.Filled.Stop,
                    onClick = onStop,
                    isPrimary = false,
                    tint = MaterialTheme.colorScheme.error
                )
            }
            DownloadState.STOPPED -> {
                ActionButton(
                    text = "开始", // stringResource(R.string.button_start),
                    icon = Icons.Filled.PlayArrow,
                    onClick = onStart,
                    isPrimary = true
                )
                Spacer(modifier = Modifier.width(8.dp))
                ActionButton(
                    text = "移除", // stringResource(R.string.button_remove),
                    icon = Icons.Filled.DeleteOutline, // 或 Icons.Filled.DeleteForever
                    onClick = onRemove,
                    isPrimary = false,
                    tint = MaterialTheme.colorScheme.error // 移除按钮也用警示色
                )
            }
            DownloadState.COMPLETED -> {
                ActionButton(
                    text = "打开", // stringResource(R.string.button_open),
                    icon = Icons.Filled.FileOpen,
                    onClick = onOpenFile,
                    isPrimary = true
                )
                Spacer(modifier = Modifier.width(8.dp))
                ActionButton(
                    text = "清除", // stringResource(R.string.button_clear),
                    icon = Icons.Filled.Clear, // 清除已完成项的图标
                    onClick = onRemove, // 此处复用 onRemove，也可定义单独的 onClear
                    isPrimary = false
                )
            }
            DownloadState.ERROR -> {
                ActionButton(
                    text = "重试", // stringResource(R.string.button_retry),
                    icon = Icons.Filled.Refresh,
                    onClick = onRetry,
                    isPrimary = true
                )
                Spacer(modifier = Modifier.width(8.dp))
                ActionButton(
                    text = "移除", // stringResource(R.string.button_remove),
                    icon = Icons.Filled.DeleteOutline,
                    onClick = onRemove,
                    isPrimary = false,
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

// 封装的操作按钮，区分主要和次要样式
@Composable
private fun ActionButton(
    text: String, // 按钮文本
    icon: ImageVector, // 按钮图标
    onClick: () -> Unit, // 点击回调
    modifier: Modifier = Modifier,
    isPrimary: Boolean = true, // 是否为主要按钮
    tint: Color = LocalContentColor.current // 图标和文本颜色（主要用于次要按钮）
) {
    if (isPrimary) {
        Button(onClick = onClick, modifier = modifier) { // 主要按钮使用 FilledButton
            Icon(icon, contentDescription = text, Modifier.size(ButtonDefaults.IconSize))
            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
            Text(text)
        }
    } else {
        OutlinedButton(onClick = onClick, modifier = modifier) { // 次要按钮使用 OutlinedButton
            Icon(icon, contentDescription = text, Modifier.size(ButtonDefaults.IconSize), tint = tint)
            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
            Text(text, color = tint)
        }
    }
}


fun formatBytes(bytes: Long): String {
    if (bytes < 0) return "0 B" // 处理负数或无效输入
    val units = arrayOf("B", "KB", "MB", "GB", "TB") // units
    var i = 0
    var value = bytes.toDouble()
    while (value >= 1024 && i < units.size - 1) {
        value /= 1024.0
        i++
    }
    return String.format(Locale.getDefault(), "%.2f %s", value, units[i])
}

// 计算预计剩余时间的辅助函数
fun calculateEtr(totalBytes: Long, downloadedBytes: Long, speedKbps: Float): String {
    if (speedKbps <= 0 || downloadedBytes >= totalBytes || totalBytes <= 0) {
        return ""
    }

    val speedBps = speedKbps * 1024
    if (speedBps <= 0) return "" // 避免除以零

    val remainingBytes = totalBytes - downloadedBytes
    val remainingSeconds = (remainingBytes / speedBps).toLong()

    if (remainingSeconds < 0) return "" // 理论上不应发生

    val hours = remainingSeconds / 3600
    val minutes = (remainingSeconds % 3600) / 60
    val seconds = remainingSeconds % 60

    return buildString { // 构建ETR字符串
        if (hours > 0) append("${hours}小时 ")
        if (minutes > 0 || hours > 0) append("${minutes}分钟 ")
        append("${seconds}秒")
    }.trim().ifEmpty { "< 1秒" }
}

// 获取用户友好的状态文本（理想情况下从字符串资源获取）
fun getStatusText(state: DownloadState, errorMessage: String?): String {
    // 在实际应用中，应使用 stringResource(id) 从 strings.xml 加载
    return when (state) {
        DownloadState.PENDING -> "等待中..."
        DownloadState.DOWNLOADING -> "下载中"
        DownloadState.PAUSED -> "已暂停"
        DownloadState.STOPPED -> "已停止"
        DownloadState.COMPLETED -> "已完成"
        DownloadState.ERROR -> "错误: ${errorMessage ?: "未知错误"}" // 如果有错误信息则显示，否则显示通用错误文本
    }
}

@Composable
fun getStatusVisuals(state: DownloadState): Pair<ImageVector, Color> {
    return when (state) {
        DownloadState.PENDING -> Icons.Filled.HourglassEmpty to MaterialTheme.colorScheme.onSurfaceVariant // 等待中
        DownloadState.DOWNLOADING -> Icons.Filled.Download to MaterialTheme.colorScheme.primary // 下载中
        DownloadState.PAUSED -> Icons.Filled.PauseCircleOutline to MaterialTheme.colorScheme.onSurfaceVariant // 已暂停
        DownloadState.STOPPED -> Icons.Filled.RemoveCircleOutline to MaterialTheme.colorScheme.onSurfaceVariant // 已停止
        DownloadState.COMPLETED -> Icons.Filled.CheckCircleOutline to Color(0xFF4CAF50) // 已完成 (使用一个明确的绿色)
        DownloadState.ERROR -> Icons.Filled.ErrorOutline to MaterialTheme.colorScheme.error // 错误
    }
}



fun formatSpeed(speedKbps: Double): String {
    if (speedKbps < 0) return "0 B/s"

    var value = speedKbps * 1024.0
    val units = arrayOf("B/s", "KB/s", "MB/s", "GB/s", "TB/s")
    var i = 0

    while (value >= 1024 && i < units.size - 1) {
        value /= 1024.0
        i++
    }
    return String.format(Locale.getDefault(), "%.2f %s", value, units[i])
}