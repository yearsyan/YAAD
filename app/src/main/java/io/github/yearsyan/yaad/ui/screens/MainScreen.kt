package io.github.yearsyan.yaad.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.LifecycleCoroutineScope
import io.github.yaad.downloader_core.HttpDownloadSession
import io.github.yaad.downloader_core.IDownloadListener
import io.github.yearsyan.yaad.ui.components.DownloadCard
import io.github.yearsyan.yaad.ui.theme.YAADTheme
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun MainScreen(lifecycleScope: LifecycleCoroutineScope) {
    var text by remember { mutableStateOf("") }
    var start by remember { mutableStateOf(false) }
    var session by remember { mutableStateOf<HttpDownloadSession?>(null) }
    val context = LocalContext.current

    YAADTheme {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            Column(
                modifier =
                    Modifier.fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text)
                Button(
                    onClick = {
                        lifecycleScope.launch(Dispatchers.IO) {
                            val saveFile =
                                File(
                                    context.filesDir,
                                    "debian-12.10.0-amd64-netinst.iso"
                                )
                            val downloadSession =
                                HttpDownloadSession(
                                    url =
                                        "https://cdimage.debian.org/debian-cd/current/amd64/iso-cd/debian-12.10.0-amd64-netinst.iso",
                                    path = saveFile.absolutePath
                                )
                            withContext(Dispatchers.Main) {
                                start = true
                                session = downloadSession
                                downloadSession.addDownloadListener(
                                    object : IDownloadListener {
                                        override fun onComplete() {
                                            lifecycleScope.launch(
                                                Dispatchers.Main
                                            ) {
                                                start = false
                                            }
                                        }
                                    }
                                )
                            }
                            downloadSession.start()
                        }
                    },
                    enabled = !start
                ) {
                    Text("Download")
                }
                session?.let {
                    DownloadCard(
                        scope = lifecycleScope,
                        downloadSession = it,
                        fileName = "debian-12.10.0-amd64-netinst.iso"
                    )
                }
            }
        }
    }
}
