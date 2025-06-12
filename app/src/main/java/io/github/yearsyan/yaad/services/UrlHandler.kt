package io.github.yearsyan.yaad.services

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.net.toUri
import com.kongzue.dialogx.dialogs.PopNotification
import com.kongzue.dialogx.dialogs.PopTip
import com.kongzue.dialogx.dialogs.WaitDialog
import io.github.yaad.downloader_core.getSystemUserAgent
import io.github.yearsyan.yaad.R
import io.github.yearsyan.yaad.downloader.DownloadManager
import io.github.yearsyan.yaad.model.VideoInfo
import io.github.yearsyan.yaad.utils.getFileInfo
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.compression.ContentEncoding
import io.ktor.client.plugins.cookies.AcceptAllCookiesStorage
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.http.headers
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object UrlHandler {

    fun createClient(): HttpClient {
        return HttpClient(CIO) {
            install(HttpTimeout) {
                requestTimeoutMillis = 30_000
                connectTimeoutMillis = 30_000
                socketTimeoutMillis = 3600_000
            }
            install(ContentEncoding) {
                gzip()
                deflate()
            }
            install(HttpCookies) {
                storage = AcceptAllCookiesStorage()
            }
            headers {
                append("User-Agent", getSystemUserAgent())
            }
            followRedirects = true
        }
    }

    fun isHttpLink(str: String): Boolean {
        try {
            val url = URL(str)
            return url.protocol == "http" || url.protocol == "https"
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    fun canRoute(context: Context, str: String): Boolean {
        try {
            val uri = str.toUri()
            val intent = Intent(Intent.ACTION_VIEW, uri)
            val packageManager = context.packageManager
            return intent.resolveActivity(packageManager) != null
        } catch (_: Exception) {
            return false
        }
    }

    suspend fun dealWithLink(
        context: Context,
        link: String,
        showExtractInfo: (extractInfo: VideoInfo) -> Unit
    ) {
        if (link.startsWith("http://") || link.startsWith("https://")) {
            WaitDialog.show(R.string.url_extracting)
            try {
                val info = getFileInfo(link)
                val contentType = info.contentType
                Log.d("dealWithLink", "contentType: $contentType")
                if (contentType.startsWith("text/html")) {
                    val resp =
                        ExtractorClient.getInstance()
                            .extractMedia(context, link, mapOf())
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
                        startResultListener = { e -> WaitDialog.dismiss() }
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
