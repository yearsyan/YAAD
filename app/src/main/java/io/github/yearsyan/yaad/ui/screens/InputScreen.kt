package io.github.yearsyan.yaad.ui.screens

import android.content.Context
import android.util.Log
import android.view.View
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kongzue.dialogx.dialogs.BottomDialog
import com.kongzue.dialogx.dialogs.PopNotification
import com.kongzue.dialogx.dialogs.PopTip
import com.kongzue.dialogx.dialogs.WaitDialog
import com.kongzue.dialogx.interfaces.OnBindView
import io.github.yearsyan.yaad.R
import io.github.yearsyan.yaad.downloader.DownloadManager
import io.github.yearsyan.yaad.model.VideoInfo
import io.github.yearsyan.yaad.services.ExtractorClient
import io.github.yearsyan.yaad.ui.components.VideoInfoView
import io.github.yearsyan.yaad.utils.ClipboardUtil
import io.github.yearsyan.yaad.utils.SettingsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private fun showVideoInfo(src: String, videoInfo: VideoInfo) {
    BottomDialog.show(
        object :
            OnBindView<BottomDialog?>(R.layout.layout_compose) {
            override fun onBind(
                dialog: BottomDialog?,
                v: View
            ) {
                val composeView =
                    v.findViewById<ComposeView>(
                        R.id.compose_view
                    )
                composeView.setContent {
                    VideoInfoView(src, videoInfo, { dialog?.dismiss() })
                }
            }
        }
    )
}

private suspend fun dealWithLink(context: Context, link: String) {
    if (link.contains("bilibili")) {
        withContext(Dispatchers.Main) {
            WaitDialog.show(R.string.url_extracting)
        }
        try {
            val options = mutableMapOf<String,String>()
            SettingsManager.getInstance(context).getCurrentCookieFile()?.let {
                options.put("--cookies", it.path)
            }
            val resp =
                ExtractorClient.getInstance().extractMedia(link, options)
            resp?.result?.let { result ->
                withContext(Dispatchers.Main) {
                    WaitDialog.dismiss()
                    showVideoInfo(link, result)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main) {
                WaitDialog.dismiss()
                PopNotification.show(R.string.extract_fail)
            }
        }
        return
    }
    if (link.startsWith("http://") || link.startsWith("https://")) {
        withContext(Dispatchers.Main) {
            WaitDialog.show(R.string.url_extracting)
        }
        DownloadManager.addHttpDownloadTask(
            url = link,
            headers = emptyMap(),
            startResultListener = { e ->
                WaitDialog.dismiss()
            }
        )
    } else if (link.startsWith("magnet:")) {
        // TODO add bt download
    } else if (link.isEmpty()) {
        PopTip.show(R.string.url_empty)
    } else {
        PopTip.show(R.string.url_format_error)
    }
}

@Composable
fun InputScreen(scope: CoroutineScope) {

    var urlText by remember { mutableStateOf("") }
    var analyzing by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val downloadWithLink: (text: String) -> Unit = downloadWithLink@{ it ->
        scope.launch {
            analyzing = true
            withContext(Dispatchers.Default) {
                dealWithLink(context, it)
            }
            analyzing = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier =
                Modifier.fillMaxWidth()
                    .padding(top = 80.dp, start = 24.dp, end = 24.dp)
        ) {
            Text(text = "Url", fontSize = 36.sp, fontWeight = FontWeight.Bold)

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                enabled = !analyzing,
                value = urlText,
                onValueChange = { urlText = it },
                label = { Text("URL") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        Column(
            modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            FloatingActionButton(
                onClick = {
                    val text = ClipboardUtil.readText(context)
                    if (text.isBlank()) {
                        PopTip.show(R.string.noting)
                        return@FloatingActionButton
                    }
                },
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Icon(Icons.Outlined.ContentPaste, contentDescription = "Paste")
            }

            FloatingActionButton(
                onClick = { downloadWithLink(urlText) },
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ) {
                if (analyzing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Icon(
                        Icons.Outlined.Download,
                        contentDescription = "Download"
                    )
                }
            }
        }
    }
}
