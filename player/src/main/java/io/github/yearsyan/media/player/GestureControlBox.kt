package io.github.yearsyan.media.player

import android.content.Context
import android.media.AudioManager
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Dragging enums remain the same
private enum class DragDirection {
    Horizontal,
    Vertical,
    None
}

private enum class VerticalDragArea {
    Left,
    Right,
    None
}

@Composable
fun GestureControlBox(
    modifier: Modifier = Modifier,
    onRequestPanelTap: () -> Unit = {},
    onRequestPlayStateTap: () -> Unit = {},
    onStartAccelerate: () -> Unit = {},
    onEndAccelerate: () -> Unit = {},
    volumeGetter: () -> Float = { 0f },
    volumeSetter: (Float) -> Unit = {},
) {
    val context = LocalContext.current

    // --- State Management ---
    var isAccelerating by remember { mutableStateOf(false) }
    var currentProgress by remember { mutableFloatStateOf(0.5f) }
    var currentBrightness by remember {
        mutableFloatStateOf(context.getCurrentBrightness())
    }
    var currentVolume by remember { mutableFloatStateOf(volumeGetter()) }
    val haptic = LocalHapticFeedback.current
    var showControlVol by remember { mutableStateOf(false) }
    var showControlBrightness by remember { mutableStateOf(false) }
    var cancelJob by remember { mutableStateOf<Job?>(null) }

    Box(
        modifier =
            modifier.pointerInput(Unit) {
                coroutineScope {
                    // ✅ 使用两个并行的手势检测器

                    // 1. 用于检测单击、双击和长按
                    launch {
                        detectTapGestures(
                            onTap = { onRequestPanelTap() },
                            onDoubleTap = { onRequestPlayStateTap() },
                            onLongPress = {
                                isAccelerating = true
                                haptic.performHapticFeedback(
                                    HapticFeedbackType.LongPress
                                )
                                onStartAccelerate()
                            },
                            onPress = {
                                try {
                                    awaitRelease()
                                } finally {
                                    if (isAccelerating) {
                                        onEndAccelerate()
                                        isAccelerating = false
                                    }
                                }
                            }
                        )
                    }

                    // 2. 用于检测水平和垂直拖动
                    launch {
                        var dragDirection = DragDirection.None
                        var verticalDragArea = VerticalDragArea.None

                        detectDragGestures(
                            onDragStart = { offset ->
                                // 根据起始位置判断是左侧还是右侧
                                verticalDragArea =
                                    if (offset.x < size.width / 2) {
                                        VerticalDragArea.Left
                                    } else {
                                        VerticalDragArea.Right
                                    }
                            },
                            onDragEnd = {
                                // 拖动结束，重置状态
                                dragDirection = DragDirection.None
                                cancelJob =
                                    launch(Dispatchers.Main) {
                                        delay(2000)
                                        showControlBrightness = false
                                        showControlVol = false
                                    }
                            },
                            onDragCancel = {
                                dragDirection = DragDirection.None
                                cancelJob =
                                    launch(Dispatchers.Main) {
                                        delay(2000)
                                        showControlBrightness = false
                                        showControlVol = false
                                    }
                            },
                            onDrag = { change, dragAmount ->
                                // 消费事件，防止传递给 tap 检测器
                                change.consume()

                                if (dragDirection == DragDirection.None) {
                                    // 首次移动，判断主要方向
                                    if (abs(dragAmount.x) > abs(dragAmount.y)) {
                                        dragDirection = DragDirection.Horizontal
                                        // gestureStatusText = "调整进度"
                                    } else {
                                        dragDirection = DragDirection.Vertical
                                        cancelJob?.cancel()
                                        if (
                                            verticalDragArea ==
                                                VerticalDragArea.Left
                                        ) {
                                            showControlBrightness = true
                                            showControlVol = false
                                            "调整亮度"
                                        } else {
                                            showControlBrightness = false
                                            showControlVol = true
                                            "调整音量"
                                        }
                                    }
                                }

                                // 根据已确定的方向处理拖动
                                when (dragDirection) {
                                    DragDirection.Horizontal -> {
                                        val delta = dragAmount.x
                                        val progressDelta = delta / size.width
                                        currentProgress =
                                            (currentProgress + progressDelta)
                                                .coerceIn(0f, 1f)
                                    }
                                    DragDirection.Vertical -> {
                                        // 垂直拖动，向上为负，向下为正，我们反转一下
                                        val delta = -dragAmount.y
                                        val changeRatio =
                                            delta / (size.height * 0.8f)

                                        when (verticalDragArea) {
                                            VerticalDragArea.Left -> {
                                                currentBrightness =
                                                    (currentBrightness +
                                                            changeRatio)
                                                        .coerceIn(0.01f, 1f)
                                                setSystemBrightness(
                                                    context,
                                                    (currentBrightness * 255)
                                                        .toInt()
                                                )
                                            }
                                            VerticalDragArea.Right -> {
                                                currentVolume =
                                                    (volumeGetter() +
                                                            changeRatio)
                                                        .coerceIn(0f, 1f)
                                                volumeSetter(currentVolume)
                                            }
                                            else -> Unit
                                        }
                                    }
                                    else -> Unit
                                }
                            }
                        )
                    }
                }
            }
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (isAccelerating) {
                Box(
                    modifier =
                        Modifier.clip(RoundedCornerShape(12.dp))
                            .align(Alignment.Center)
                ) {
                    Row { Text("🚀", fontSize = 48.sp) }
                }
            }

            if (showControlVol || showControlBrightness) {
                Box(
                    modifier =
                        Modifier.align(Alignment.Center)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0, 0, 0, 30))
                ) {
                    val icon =
                        if (showControlVol) Icons.AutoMirrored.Filled.VolumeUp
                        else Icons.Default.LightMode
                    val radio =
                        if (showControlVol) currentVolume else currentBrightness
                    Row(
                        modifier =
                            Modifier.padding(
                                horizontal = 12.dp,
                                vertical = 4.dp
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = icon, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier =
                                Modifier.height(6.dp)
                                    .width(64.dp)
                                    .clip(RoundedCornerShape(3.dp))
                        ) {
                            Box(
                                modifier =
                                    Modifier.fillMaxSize()
                                        .background(Color.Gray)
                            )
                            Box(
                                modifier =
                                    Modifier.fillMaxHeight()
                                        .background(
                                            MaterialTheme.colorScheme.primary
                                        )
                                        .align(Alignment.CenterStart)
                                        .fillMaxWidth(radio)
                            )
                        }
                    }
                }
            }
        }
    }
}

// Helper functions remain the same
private fun Context.getCurrentBrightness(): Float {
    return try {
        Settings.System.getInt(
            contentResolver,
            Settings.System.SCREEN_BRIGHTNESS
        ) / 255f
    } catch (e: Exception) {
        0.5f
    }
}

private fun Context.getCurrentVolume(): Float {
    val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
    val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
    val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
    return currentVolume.toFloat() / maxVolume
}

private fun setSystemBrightness(context: Context, brightness: Int): Boolean {
    return try {
        if (!Settings.System.canWrite(context)) {
            // Manifest.permission.WRITE_SETTINGS
            return false
        }
        Settings.System.putInt(
            context.contentResolver,
            Settings.System.SCREEN_BRIGHTNESS,
            brightness.coerceIn(0, 255)
        )
        true
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}
