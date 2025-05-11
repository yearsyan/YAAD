package io.github.yearsyan.yaad.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.LifecycleCoroutineScope
import io.github.yaad.downloader_core.HttpDownloadSession
import io.github.yearsyan.yaad.model.MediaResult
import io.github.yearsyan.yaad.ui.components.DownloadCard
import io.github.yearsyan.yaad.ui.theme.YAADTheme
import io.github.yearsyan.yaad.utils.YtDlpUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File

@Composable
fun MainScreen(lifecycleScope: LifecycleCoroutineScope) {
    var text by remember { mutableStateOf("") }
    var start by remember { mutableStateOf(false) }
    var session by remember { mutableStateOf<HttpDownloadSession?>(null) }
    val context = LocalContext.current

    YAADTheme {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            Column (
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text)
                Button({
                    lifecycleScope.launch(Dispatchers.IO){
                        val jsonText = YtDlpUtil.callYtDlp(context, arrayOf("-J", "https://www.bilibili.com/video/BV16h5NzqE2K"))
                        val mediaResult = Json.decodeFromString<MediaResult>(jsonText)
                        withContext(Dispatchers.Main) {
                            text = jsonText
                        }
                    }
                }) {
                    Text("Req")
                }
                Button(
                    onClick = {
                        lifecycleScope.launch (Dispatchers.IO) {
                            val downloadSession = HttpDownloadSession(
                                url = "https://mirrors.aliyun.com/archlinux/iso/2025.05.01/archlinux-2025.05.01-x86_64.iso",
                                path = File(context.filesDir, "archlinux-2025.05.01-x86_64.iso").absolutePath
                            )
                            withContext(Dispatchers.Main) {
                                start = true
                                session = downloadSession
                            }
                            downloadSession.start()
                        }
                    },
                    enabled = !start
                ) {
                    Text("download")
                }
                session?.let { DownloadCard(
                    downloadSession = it,
                    fileName = "archlinux-2025.05.01-x86_64.iso"
                ) }
            }
        }
    }
}