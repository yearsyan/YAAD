package io.github.yearsyan.yaad.ui.player

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import io.github.yearsyan.media.player.VideoPlayer
import io.github.yearsyan.yaad.ui.theme.YAADTheme

class PlayerActivity : ComponentActivity() {

    private lateinit var player: Player
    private var isFullScreen = mutableStateOf(false)

    private fun createMediaItemFromIntent(): MediaItem? {
        val uri = intent.getStringExtra("video_uri")
        if (uri?.isNotEmpty() == true) {
            return MediaItem.fromUri(uri)
        }
        return null
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        isFullScreen.value = true
        val mediaItem = createMediaItemFromIntent()
        if (mediaItem == null) {
            handleResultError()
            return
        }
        player =
            ExoPlayer.Builder(this).build().apply {
                setMediaItem(mediaItem)
                prepare()
                playWhenReady = true
            }
        setContent {
            YAADTheme {
                Scaffold { innerPadding ->
                    Column(
                        modifier = Modifier.padding(innerPadding).fillMaxSize()
                    ) {
                        VideoPlayer(
                            player = player,
                            modifier = Modifier.fillMaxWidth(),
                            isFullScreen = isFullScreen.value,
                            requestFullScreenUpdate = { fullScreen ->
                                isFullScreen.value = fullScreen
                            }
                        )
                    }
                }
            }
        }

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    // fixme: 预测性返回
                    if (isFullScreen.value) {
                        isFullScreen.value = false
                    } else {
                        isEnabled = false
                        this@PlayerActivity.onBackPressedDispatcher
                            .onBackPressed()
                        isEnabled = true
                    }
                }
            }
        )
    }

    private fun handleResultError() {
        setContent {
            YAADTheme {
                Scaffold(contentColor = Color.Black) { innerPadding ->
                    Column(
                        modifier = Modifier.padding(innerPadding).fillMaxSize()
                    ) {
                        Text(
                            text = "Result Error",
                            color = Color.White,
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        player.release()
    }

    companion object {
        fun startWithUri(context: Context, uri: String) {
            val intent =
                Intent(context, PlayerActivity::class.java).apply {
                    putExtra("video_uri", uri)
                }
            context.startActivity(intent)
        }
    }
}
