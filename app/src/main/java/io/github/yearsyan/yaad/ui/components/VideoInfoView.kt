package io.github.yearsyan.yaad.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kongzue.dialogx.dialogs.PopNotification
import io.github.yearsyan.yaad.R
import io.github.yearsyan.yaad.downloader.DownloadManager
import io.github.yearsyan.yaad.model.VideoInfo
import io.github.yearsyan.yaad.utils.FileUtils
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun VideoInfoView(src: String, videoInfo: VideoInfo, finish: () -> Unit) {
    var loading by remember { mutableStateOf(false) }
    val context = LocalContext.current

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
            stringResource(R.string.video_title, videoInfo.title),
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            stringResource(R.string.video_source, videoInfo.site),
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            stringResource(R.string.select_quality),
            style = MaterialTheme.typography.titleMedium
        )
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
                    val items = it.src.map { streamItem -> streamItem.get(0) }
                    DownloadManager.addExtractedMediaDownloadTask(
                        videoInfo.title,
                        src,
                        items,
                        videoInfo.requestHeaders
                    ) { sessionRecord, media ->
                        val mergedFile = File(media)
                        if (mergedFile.exists()) {
                            val fileName = "${videoInfo.title}.mp4"
                            val success =
                                withContext(Dispatchers.IO) {
                                    FileUtils.moveToDownloads(
                                        context,
                                        mergedFile,
                                        fileName
                                    )
                                }
                            PopNotification.show(
                                context.getString(
                                    if (success) {
                                        R.string.video_saved
                                    } else {
                                        R.string.move_to_download_failed
                                    }
                                )
                            )
                        } else {
                            PopNotification.show(
                                context.getString(R.string.merge_failed)
                            )
                        }
                    }
                    finish()
                }
            }
        ) {
            Text(stringResource(R.string.download))
        }
    }
}
