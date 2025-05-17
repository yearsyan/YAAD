package io.github.yearsyan.yaad.ui.screens

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kongzue.dialogx.dialogs.PopNotification
import com.kongzue.dialogx.dialogs.PopTip
import io.github.yearsyan.yaad.R
import io.github.yearsyan.yaad.downloader.DownloadManager
import io.github.yearsyan.yaad.utils.ClipboardUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun InputScreen(scope: CoroutineScope) {

    var urlText by remember { mutableStateOf("") }
    var analyzing by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val downloadWithLink: (text: String) -> Unit = downloadWithLink@{
        if (
            it.startsWith("http://") ||
            it.startsWith("https://")
        ) {
            if (analyzing) {
                return@downloadWithLink
            }
            analyzing = true
            DownloadManager.addHttpDownloadTask(
                url = it,
                headers = emptyMap(),
                startResultListener = { e ->
                    scope.launch {
                        e?.let {
                            PopNotification.show(context.getString(R.string.task_add_fail) + it.message)
                        } ?: run {
                            PopNotification
                                .build()
                                .setMessage(R.string.task_add_success)
                                .setOnPopNotificationClickListener { dialog, v ->
                                    true
                                }
                            PopNotification.show(R.string.task_add_success)
                        }
                        analyzing = false
                    }
                }
            )
        } else if (it.startsWith("magnet:")) {
            // TODO add bt download
        } else if (it.isEmpty()) {
            PopTip.show(R.string.url_empty)
        } else {
            PopTip.show(R.string.url_format_error)
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
                onClick = {
                    downloadWithLink(urlText)
                },
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ) {
                if (analyzing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Icon(Icons.Outlined.Download, contentDescription = "Download")
                }
            }
        }
    }
}
