package io.github.yearsyan.yaad.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.dp
import io.github.yaad.downloader_core.HttpDownloadStatus

@Composable
fun MultiThreadProgressBar(
    status: HttpDownloadStatus,
    total: Long,
    modifier: Modifier = Modifier
) {
    val radius = 4.dp
    val backgroundColor = ProgressIndicatorDefaults.linearTrackColor
    val indicatorColor = ProgressIndicatorDefaults.linearColor

    Canvas(
        modifier =
            modifier
                .fillMaxWidth()
                .background(backgroundColor, shape = RoundedCornerShape(radius))
    ) {
        val widthPerByte = size.width / total
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
            status.parts.forEach { part ->
                val startX =
                    ((part.start - status.parts.minOf { it.start }) *
                        widthPerByte)
                val downloadedWidth = (part.downloaded * widthPerByte)

                drawRoundRect(
                    color = indicatorColor,
                    topLeft = Offset(startX, 0f),
                    size = Size(downloadedWidth, size.height),
                )
            }
        }
    }
}
