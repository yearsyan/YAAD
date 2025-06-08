package io.github.yearsyan.yaad.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.dp
import io.github.yaad.downloader_core.DownloadState
import io.github.yearsyan.yaad.downloader.DownloadManager
import kotlinx.coroutines.delay

@Composable
fun ExtractDownloadProgressBar(
    record: DownloadManager.ExtractedMediaDownloadSessionRecord,
    modifier: Modifier = Modifier
) {
    val radius = 4.dp
    val backgroundColor = ProgressIndicatorDefaults.linearTrackColor
    val indicatorColor = ProgressIndicatorDefaults.linearColor
    val currentProgress by
        produceState(initialValue = listOf()) {
            while (true) {
                value =
                    record.childSessions
                        .mapNotNull { it.httpDownloadSession?.getStatus() }
                        .map { child ->
                            child.parts.map {
                                Pair(it.downloaded, it.end - it.start)
                            }
                        }
                        .flatten()
                if (record.downloadState == DownloadState.COMPLETED) {
                    return@produceState
                }
                delay(500)
            }
        }

    Canvas(
        modifier =
            modifier.background(
                backgroundColor,
                shape = RoundedCornerShape(radius)
            )
    ) {
        if (
            currentProgress.isNotEmpty() &&
                !currentProgress.any { it.second == 0L }
        ) {
            val widthPerByte =
                size.width /
                    currentProgress
                        .map { it.second }
                        .reduce { acc, l -> acc + l }
            val radiusPx = radius.toPx()
            val path =
                Path().apply {
                    addRoundRect(
                        RoundRect(
                            rect = Rect(Offset.Zero, size),
                            cornerRadius = CornerRadius(radiusPx, radiusPx)
                        )
                    )
                }
            clipPath(path) {
                var offset = 0f
                currentProgress.forEach { part ->
                    val startX = offset
                    val downloadedWidth = (part.first * widthPerByte)
                    drawRoundRect(
                        color = indicatorColor,
                        topLeft = Offset(startX, 0f),
                        size = Size(downloadedWidth, size.height),
                    )
                    offset += part.second * widthPerByte
                }
            }
        }
    }
}
