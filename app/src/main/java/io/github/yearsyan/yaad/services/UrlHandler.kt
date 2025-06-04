package io.github.yearsyan.yaad.services

import android.content.Context
import android.util.Log
import com.kongzue.dialogx.dialogs.PopNotification
import com.kongzue.dialogx.dialogs.PopTip
import com.kongzue.dialogx.dialogs.WaitDialog
import io.github.yearsyan.yaad.R
import io.github.yearsyan.yaad.downloader.DownloadManager
import io.github.yearsyan.yaad.model.VideoInfo
import io.github.yearsyan.yaad.utils.getFileInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object UrlHandler {
    suspend fun dealWithLink(context: Context, link: String, showExtractInfo: (extractInfo: VideoInfo) -> Unit) {
        if (link.startsWith("http://") || link.startsWith("https://")) {
            WaitDialog.show(R.string.url_extracting)
            try {
                val info = getFileInfo(link)
                val contentType = info.contentType
                Log.d("dealWithLink", "contentType: $contentType")
                if (contentType.startsWith("text/html")) {
                    val resp =
                        ExtractorClient.getInstance().extractMedia(context, link, mapOf())
                    resp?.result?.let { result ->
                        withContext(Dispatchers.Main) {
                            WaitDialog.dismiss()
                            showExtractInfo(result)
                        }
                    }
                } else {
                    DownloadManager.addHttpDownloadTask(
                        url = link,
                        headers = emptyMap(),
                        startResultListener = { e ->
                            WaitDialog.dismiss()
                        }
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
                PopNotification.show(R.string.extract_fail)
            } finally {
                WaitDialog.dismiss()
            }
        } else if (link.startsWith("magnet:")) {
            // TODO add bt download
        } else if (link.isEmpty()) {
            PopTip.show(R.string.url_empty)
        } else {
            PopTip.show(R.string.url_format_error)
        }
    }

}