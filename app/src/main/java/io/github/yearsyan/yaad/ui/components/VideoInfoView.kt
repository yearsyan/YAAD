package io.github.yearsyan.yaad.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import com.kongzue.dialogx.dialogs.PopNotification
import io.github.yearsyan.yaad.downloader.DownloadManager
import io.github.yearsyan.yaad.model.VideoInfo
import io.github.yearsyan.yaad.utils.parseOriginFromReferer

@Composable
fun VideoInfoView(videoInfo: VideoInfo, finish: () -> Unit) {
    var loading by remember { mutableStateOf(false) }

    var selectedQualityKey by remember { mutableStateOf<String?>(null) }
    val containerHeightDp =
        with(LocalDensity.current) {
            (LocalWindowInfo.current.containerSize.height * 0.6).toInt().toDp()
        }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "标题：${videoInfo.title}",
            style = MaterialTheme.typography.titleLarge
        )
        Text("来自：${videoInfo.site}", style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(16.dp))

        Text("选择清晰度：", style = MaterialTheme.typography.titleMedium)
        LazyColumn(
            modifier =
                Modifier.fillMaxWidth()
                    .heightIn(min = 0.dp, max = containerHeightDp)
        ) {
            items(videoInfo.streams.keys.sorted()) { key ->
                val stream = videoInfo.streams[key]!!
                Row(
                    modifier =
                        Modifier.fillMaxWidth()
                            .clickable { selectedQualityKey = key }
                            .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedQualityKey == key,
                        onClick = { selectedQualityKey = key }
                    )
                    Text(
                        "${stream.quality}（${stream.container}，${(stream.size / 1024 / 1024)} MB）"
                    )
                }
            }
        }

        Button(
            enabled = selectedQualityKey != null && !loading,
            onClick = {
                loading = true
                val stream =
                    selectedQualityKey?.let { key -> videoInfo.streams[key] }
                stream?.let {
                    it.src.forEach { itemList ->
                        itemList.forEach { url ->
                            DownloadManager.addHttpDownloadTask(
                                url,
                                videoInfo.requestHeaders,
                                { PopNotification.show("下载开始") }
                            )
                        }
                    }
                    finish()
                }
            }
        ) {
            Text("下载")
        }
    }
}
