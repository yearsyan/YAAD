package io.github.yearsyan.media.player

import android.app.Activity
import android.content.pm.ActivityInfo
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Adb
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.media3.common.C
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Metadata
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.ui.compose.PlayerSurface
import androidx.media3.ui.compose.SURFACE_TYPE_SURFACE_VIEW
import androidx.media3.ui.compose.state.PlayPauseButtonState
import androidx.media3.ui.compose.state.PlaybackSpeedState
import androidx.media3.ui.compose.state.rememberPlayPauseButtonState
import androidx.media3.ui.compose.state.rememberPlaybackSpeedState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

fun hideSystemUI(activity: Activity) {
    val controller = activity.window.insetsController
    controller?.hide(
        WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars()
    )
    controller?.systemBarsBehavior =
        WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
}

fun showSystemUI(activity: Activity) {
    val controller = activity.window.insetsController
    controller?.show(
        WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars()
    )
}

fun formatDuration(millis: Long): String {
    val totalSeconds = millis / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60

    return if (hours > 0) {
        String.format("%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}

@Composable
fun PlayProgress(
    playPosition: Long,
    durationMs: Long,
    onRequestSeek: (Long) -> Unit = {},
    modifier: Modifier = Modifier,
    height: Dp = 4.dp
) {
    val h = 12.dp
    BoxWithConstraints(
        modifier =
            modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .height(h) // 包括进度条和拖动图标的高度
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val clickedFraction = offset.x / size.width
                        val seekPosition =
                            (clickedFraction * durationMs)
                                .toLong()
                                .coerceIn(0, durationMs)
                        onRequestSeek(seekPosition)
                    }
                }
    ) {
        val progressFraction = playPosition.toFloat() / durationMs
        Box(
            modifier =
                Modifier.fillMaxWidth()
                    .height(height)
                    .align(Alignment.CenterStart)
                    .clip(RoundedCornerShape(3.dp))
                    .background(MaterialTheme.colorScheme.background)
        )
        Box(
            modifier =
                Modifier.fillMaxWidth(progressFraction)
                    .height(height)
                    .align(Alignment.CenterStart)
                    .background(MaterialTheme.colorScheme.primary)
        )

        Box(
            modifier =
                Modifier.fillMaxWidth(progressFraction)
                    .align(Alignment.CenterStart)
                    .height(h)
        ) {
            Icon(
                Icons.Default.Adb,
                contentDescription = null,
                modifier =
                    Modifier.size(h).align(Alignment.CenterEnd).offset(x = 6.dp)
            )
        }
    }
}

enum class DragMode {
    HORIZONTAL,
    VERTICAL
}

@Composable
private fun NormalPanel(
    requestFullScreenUpdate: (Boolean) -> Unit = {},
    modifier: Modifier,
    doSeek: (ts: Long) -> Unit,
    contentColor: Color,
    state: PlayPauseButtonState,
    durationMs: Long,
    positionMs: Long
) {
    val normalIconSize = 28.dp
    val icon =
        if (state.showPlay) Icons.Default.PlayArrow else Icons.Default.Pause

    Box(modifier = modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            IconButton(
                onClick = state::onClick,
                modifier = Modifier.size(normalIconSize),
                enabled = state.isEnabled
            ) {
                Icon(
                    modifier = Modifier.size(normalIconSize),
                    imageVector = icon,
                    contentDescription =
                        if (state.showPlay) {
                            "play"
                        } else {
                            "pause"
                        },
                    tint = contentColor
                )
            }

            Box(modifier = Modifier.weight(1f).padding(horizontal = 5.dp)) {
                PlayProgress(
                    playPosition = positionMs,
                    durationMs = durationMs,
                    modifier = Modifier.fillMaxWidth(),
                    height = 3.dp,
                    onRequestSeek = doSeek
                )
            }

            IconButton(
                onClick = { requestFullScreenUpdate(true) },
                modifier = Modifier.size(normalIconSize),
                enabled = state.isEnabled
            ) {
                Icon(
                    modifier = Modifier.size(normalIconSize),
                    imageVector = Icons.Default.Fullscreen,
                    contentDescription = "fullscreen",
                    tint = contentColor
                )
            }
        }
    }
}

@Composable
private fun FullscreenPanel(
    requestFullScreenUpdate: (Boolean) -> Unit = {},
    modifier: Modifier,
    doSeek: (ts: Long) -> Unit,
    title: String = "",
    contentColor: Color,
    state: PlayPauseButtonState,
    playbackSpeed: PlaybackSpeedState,
    durationMs: Long,
    positionMs: Long
) {

    val icon =
        if (state.showPlay) Icons.Default.PlayArrow else Icons.Default.Pause

    Box(modifier = modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(horizontalArrangement = Arrangement.SpaceBetween) {
                IconButton(
                    onClick = { requestFullScreenUpdate(false) },
                    enabled = state.isEnabled
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBackIosNew,
                        contentDescription = "back",
                        tint = contentColor
                    )
                }
                if (title.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(title, color = contentColor)
                }
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text =
                    "${formatDuration(positionMs)}/${formatDuration(durationMs)}",
                color = contentColor,
                style = MaterialTheme.typography.labelMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            PlayProgress(
                playPosition = positionMs,
                durationMs = durationMs,
                modifier = Modifier.fillMaxWidth(),
                onRequestSeek = doSeek
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = state::onClick,
                        enabled = state.isEnabled
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription =
                                if (state.showPlay) {
                                    "play"
                                } else {
                                    "pause"
                                },
                            tint = contentColor
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "x${playbackSpeed.playbackSpeed}",
                        color = contentColor
                    )
                }
            }
        }
    }
}

@Composable
fun VideoPlayer(
    player: Player,
    modifier: Modifier = Modifier,
    title: String = "",
    requestFullScreenUpdate: (Boolean) -> Unit = {},
    isFullScreen: Boolean = false,
    controlShowTime: Long = 8000
) {

    val context = LocalContext.current
    val activity = context as Activity
    var listener by remember { mutableStateOf<Player.Listener?>(null) }
    var radio by remember { mutableStateOf(1f) }
    var videoTitle by remember { mutableStateOf(title) }
    val state = rememberPlayPauseButtonState(player)
    val playbackSpeed = rememberPlaybackSpeedState(player)
    val contentColor = Color.White
    var showPanel by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var hideJob by remember { mutableStateOf<Job?>(null) }
    var totalDuration by remember { mutableStateOf(C.TIME_UNSET) }
    var progressPosition by remember { mutableStateOf(0L) }
    var seekPosition by remember { mutableStateOf(-1L) }
    var stashSpeed by remember { mutableStateOf(-1f) }

    val doSeek: (ts: Long) -> Unit = { ts ->
        seekPosition = ts
        player.seekTo(ts)
    }

    val showPos =
        if (seekPosition >= 0) {
            seekPosition
        } else {
            progressPosition
        }
    val deltaVolume: (delta: Int) -> Unit = {
        val newVolume = (player.volume * 100 + it).coerceIn(0f, 100f) / 100f
        player.volume = newVolume
    }

    LaunchedEffect(title) { videoTitle = title }

    LaunchedEffect(player) {
        player.apply {
            listener =
                object : Player.Listener {

                    override fun onVideoSizeChanged(videoSize: VideoSize) {
                        super.onVideoSizeChanged(videoSize)
                        radio =
                            videoSize.width.toFloat() /
                                videoSize.height.toFloat()
                    }

                    override fun onMetadata(metadata: Metadata) {
                        super.onMetadata(metadata)
                    }

                    override fun onIsLoadingChanged(isLoading: Boolean) {
                        super.onIsLoadingChanged(isLoading)
                    }

                    override fun onMediaMetadataChanged(
                        mediaMetadata: MediaMetadata
                    ) {
                        super.onMediaMetadataChanged(mediaMetadata)
                        if (
                            title.isEmpty() &&
                                mediaMetadata.title.isNullOrEmpty()
                        ) {
                            videoTitle = mediaMetadata.title.toString()
                        }
                    }

                    override fun onPlaybackStateChanged(playbackState: Int) {
                        super.onPlaybackStateChanged(playbackState)
                        if (playbackState == Player.STATE_READY) {
                            val durationMs = player.duration
                            if (durationMs != C.TIME_UNSET) {
                                totalDuration = durationMs
                            }
                        }
                    }

                    override fun onPositionDiscontinuity(
                        oldPosition: Player.PositionInfo,
                        newPosition: Player.PositionInfo,
                        reason: Int
                    ) {
                        if (reason == Player.DISCONTINUITY_REASON_SEEK) {
                            seekPosition = -1L
                            progressPosition = newPosition.positionMs
                        }
                    }
                }
            addListener(listener!!)
        }
    }

    DisposableEffect(Unit) {
        onDispose { listener?.let { player.removeListener(it) } }
    }

    LaunchedEffect(isFullScreen) {
        if (isFullScreen) {
            activity.requestedOrientation =
                ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            hideSystemUI(activity)
        } else {
            activity.requestedOrientation =
                ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            showSystemUI(activity)
        }
    }

    LaunchedEffect(player, totalDuration) {
        while (true && isActive) {
            if (player.isPlaying) {
                progressPosition = player.currentPosition
            }
            delay(250L)
        }
    }

    BoxWithConstraints(modifier = modifier.background(Color.Black)) {
        val maxWidth = this.maxWidth
        val maxHeight = this.maxHeight
        val surfaceModifier =
            Modifier.aspectRatio(radio).align(Alignment.Center)
        if (maxWidth / maxHeight > radio) {
            surfaceModifier.fillMaxHeight()
        } else {
            surfaceModifier.fillMaxWidth()
        }

        PlayerSurface(
            player = player,
            modifier = surfaceModifier,
            surfaceType = SURFACE_TYPE_SURFACE_VIEW,
        )

        GestureControlBox(
            modifier = Modifier.matchParentSize(),
            onRequestPanelTap = {
                val nextState = !showPanel
                hideJob?.cancel()
                if (nextState) {
                    hideJob =
                        scope.launch(Dispatchers.Main) {
                            delay(controlShowTime)
                            showPanel = false
                        }
                }
                showPanel = nextState
            },
            onRequestPlayStateTap = {
                if (state.isEnabled) {
                    state.onClick()
                }
            },
            onStartAccelerate = {
                if (playbackSpeed.isEnabled) {
                    stashSpeed = playbackSpeed.playbackSpeed
                }
                playbackSpeed.updatePlaybackSpeed(3.0f)
            },
            onEndAccelerate = {
                if (playbackSpeed.isEnabled && stashSpeed >= 0) {
                    playbackSpeed.updatePlaybackSpeed(stashSpeed)
                }
            },
            volumeGetter = { player.volume },
            volumeSetter = { newVolume -> player.volume = newVolume }
        )

        if (showPanel) {
            if (isFullScreen) {
                FullscreenPanel(
                    modifier = Modifier.matchParentSize(),
                    requestFullScreenUpdate = requestFullScreenUpdate,
                    doSeek = doSeek,
                    title = videoTitle,
                    contentColor = contentColor,
                    state = state,
                    durationMs = totalDuration,
                    positionMs = showPos,
                    playbackSpeed = playbackSpeed
                )
            } else {
                NormalPanel(
                    modifier = Modifier.matchParentSize(),
                    requestFullScreenUpdate = requestFullScreenUpdate,
                    doSeek = doSeek,
                    contentColor = contentColor,
                    state = state,
                    durationMs = totalDuration,
                    positionMs = showPos
                )
            }
        }
    }
}
