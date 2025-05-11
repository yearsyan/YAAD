package io.github.yearsyan.yaad.utils

import android.content.Context
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import java.io.File
import java.io.IOException
import java.util.regex.Pattern
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.buffer
import okio.sink
import okio.source

object YtDlpUtil {

    const val YT_DLP_FILE_NAME = "yt-dlp.zip"
    private const val GITHUB_REPO = "yt-dlp/yt-dlp"
    private const val RELEASE_URL =
        "https://github.com/$GITHUB_REPO/releases/latest"
    private const val DOWNLOAD_URL_TEMPLATE =
        "https://github.com/$GITHUB_REPO/releases/download/%s/yt-dlp"

    private val client = OkHttpClient()
    private val versionPattern =
        Pattern.compile("tag/(\\d{4}\\.\\d{2}\\.\\d{2})")

    fun callYtDlp(context: Context, args: Array<String>): String {
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(context))
        }
        val py = Python.getInstance()
        val pyModule = py.getModule("run_yt_dlp")
        val dlpPath = AssetUtil.getFilePath(context, YT_DLP_FILE_NAME)
        val pyRes = pyModule.callAttr("run", arrayOf(dlpPath, *args))
        return pyRes.toString()
    }

    /** Gets the latest version number. */
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

    fun getCurrentVersion(context: Context): String {
        return callYtDlp(context, arrayOf("--version")).trim()
    }

    /** Downloads the specified version of yt-dlp and processes it. */
    suspend fun downloadVersion(
        context: Context,
        version: String,
        downloadCb: (Float) -> Unit = {}
    ): Boolean {
        val downloadUrl = String.format(DOWNLOAD_URL_TEMPLATE, version)
        val tempFile = File(context.cacheDir, "yt-dlp_temp")
        val outputFile = File(context.filesDir, YT_DLP_FILE_NAME)

        val request = Request.Builder().url(downloadUrl).build()

        return try {

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return false

                val contentLength = response.body?.contentLength() ?: 0L
                var bytesRead: Long = 0
                val bufferSize = 8 * 1024 // 8KB buffer

                response.body?.let { body ->
                    tempFile.sink().buffer().use { sink ->
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

            if (!processFileLikeTail(tempFile, outputFile)) {
                return false
            }

            tempFile.delete()

            true
        } catch (e: IOException) {
            e.printStackTrace()
            false
        }
    }

    /** Checks if yt-dlp.zip exists. */
    fun isYtDlpExists(context: Context): Boolean {
        val file = File(context.filesDir, YT_DLP_FILE_NAME)
        return file.exists()
    }

    private fun processFileLikeTail(
        inputFile: File,
        outputFile: File
    ): Boolean {
        return try {
            inputFile.source().buffer().use { source ->
                outputFile.sink().buffer().use { sink ->
                    source
                        .indexOf('\n'.code.toByte())
                        .takeIf { it != -1L }
                        ?.let { source.skip(it + 1) }
                    sink.writeAll(source)
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
