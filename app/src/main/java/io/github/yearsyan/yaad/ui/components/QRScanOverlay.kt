package io.github.yearsyan.yaad.ui.components

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun QRScanOverlay(
    modifier: Modifier = Modifier,
    onToggleFlash: (enable: Boolean) -> Boolean = { false },
    onBackClick: () -> Unit = {},
    onGallerySelected: (bitmap: Bitmap) -> Unit = {},
    boxSize: Dp = 250.dp
) {
    // 扫描线动画
    val infiniteTransition = rememberInfiniteTransition()
    val scanLinePosition by
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(1500, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                )
        )
    val context = LocalContext.current
    val getImage =
        rememberLauncherForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            uri?.let {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val source =
                        ImageDecoder.createSource(context.contentResolver, uri)
                    onGallerySelected(
                        ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                            decoder.setAllocator(
                                ImageDecoder.ALLOCATOR_SOFTWARE
                            )
                        }
                    )
                } else {
                    onGallerySelected(
                        MediaStore.Images.Media.getBitmap(
                            context.contentResolver,
                            uri
                        )
                    )
                }
            }
        }
    var isFlashOn by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        // 绘制扫描线和扩散效果
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val boxWidth = boxSize.toPx()
            val boxHeight = boxSize.toPx()

            // 计算扫描框位置（居中）
            val boxLeft = (canvasWidth - boxWidth) / 2
            val boxTop = (canvasHeight - boxHeight) / 2

            // 计算扫描线位置
            val lineY = boxTop + (boxHeight * scanLinePosition)

            // 绘制扫描线（带渐变效果）
            val lineGradient =
                Brush.verticalGradient(
                    colors =
                        listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = 0.8f),
                            Color.White,
                            Color.White.copy(alpha = 0.8f),
                            Color.Transparent
                        ),
                    startY = lineY - 20f,
                    endY = lineY + 20f
                )

            drawLine(
                brush = lineGradient,
                start = Offset(boxLeft, lineY),
                end = Offset(boxLeft + boxWidth, lineY),
                strokeWidth = 4.dp.toPx(),
                cap = StrokeCap.Round
            )

            // 绘制扫描线端点光效
            drawCircle(
                brush =
                    Brush.radialGradient(
                        colors = listOf(Color.White, Color.Transparent),
                        center = Offset(boxLeft, lineY),
                        radius = 10.dp.toPx()
                    ),
                center = Offset(boxLeft, lineY),
                radius = 10.dp.toPx()
            )

            drawCircle(
                brush =
                    Brush.radialGradient(
                        colors = listOf(Color.White, Color.Transparent),
                        center = Offset(boxLeft + boxWidth, lineY),
                        radius = 10.dp.toPx()
                    ),
                center = Offset(boxLeft + boxWidth, lineY),
                radius = 10.dp.toPx()
            )
        }

        // 返回按钮（左上角）
        IconButton(
            onClick = onBackClick,
            modifier =
                Modifier.padding(horizontal = 16.dp, vertical = 0.dp)
                    .align(Alignment.TopStart)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color.White
            )
        }

        // 底部按钮（手电筒、相册）
        Row(
            modifier =
                Modifier.fillMaxWidth()
                    .padding(32.dp)
                    .align(Alignment.BottomCenter),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = {
                    if (onToggleFlash(!isFlashOn)) {
                        isFlashOn = !isFlashOn
                    }
                },
                modifier =
                    Modifier.background(
                        color =
                            if (isFlashOn) Color.White else Color.Transparent,
                        shape = RoundedCornerShape(50)
                    ),
            ) {
                Icon(
                    imageVector = Icons.Default.FlashlightOn,
                    contentDescription = "Flashlight",
                    tint = if (isFlashOn) Color.Black else Color.White
                )
            }

            IconButton(onClick = { getImage.launch("image/*") }) {
                Icon(
                    imageVector = Icons.Default.PhotoLibrary,
                    contentDescription = "Open Gallery",
                    tint = Color.White
                )
            }
        }
    }
}
