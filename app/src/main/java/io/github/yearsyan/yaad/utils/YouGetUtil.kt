package io.github.yearsyan.yaad.utils

import android.content.Context
import java.io.File
import java.io.IOException
import java.util.regex.Pattern
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.buffer
import okio.sink

object YouGetUtil {
    const val YT_GET_FILE_NAME = "you-get.zip"
    private const val GITHUB_REPO = "yt-dlp/yt-dlp"
    private const val RELEASE_URL =
        "https://github.com/yearsyan/you-get-zipimport/releases/latest"
    private val client = OkHttpClient()
    private val versionPattern =
        Pattern.compile("tag/(\\d{4}\\.\\d{2}\\.\\d{2})")
    private const val DOWNLOAD_URL_TEMPLATE =
        "https://github.com/${GITHUB_REPO}/releases/download/%s/you-get.zip"

    fun isYouGetExists(context: Context): Boolean {
        val file = File(context.filesDir, YT_GET_FILE_NAME)
        return file.exists()
    }

    fun getLatestVersion(): String? {
        val request = Request.Builder().url(RELEASE_URL).build()

        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null

                val body = response.body?.string() ?: return null
                val matcher = versionPattern.matcher(body)
                if (matcher.find()) {
                    matcher.group(1)
                } else {
                    null
                }
            }
        } catch (e: IOException) {
            null
        }
    }

    suspend fun downloadVersion(
        context: Context,
        version: String,
        downloadCb: (Float) -> Unit = {}
    ): Boolean {
        val downloadUrl = String.format(DOWNLOAD_URL_TEMPLATE, version)
        val outputFile = File(context.filesDir, YT_GET_FILE_NAME)

        val request = Request.Builder().url(downloadUrl).build()

        return try {

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return false

                val contentLength = response.body?.contentLength() ?: 0L
                var bytesRead: Long = 0
                val bufferSize = 8 * 1024 // 8KB buffer

                response.body?.let { body ->
                    outputFile.sink().buffer().use { sink ->
                        val source = body.source()
                        val buffer = okio.Buffer()

                        while (true) {
                            val read = source.read(buffer, bufferSize.toLong())
                            if (read == -1L) break

                            sink.write(buffer, read)
                            bytesRead += read

                            if (contentLength > 0) {
                                val progress = 1.0f * bytesRead / contentLength
                                withContext(Dispatchers.Main) {
                                    downloadCb(progress)
                                }
                            }
                        }
                    }
                }
            }

            true
        } catch (e: IOException) {
            e.printStackTrace()
            false
        }
    }
}
