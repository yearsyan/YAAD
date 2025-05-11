package io.github.yearsyan.yaad.utils

import android.content.Context
import io.github.yaad.downloader_core.HttpDownloadSession
import io.github.yearsyan.yaad.utils.YouGetUtil.YT_GET_FILE_NAME
import java.io.File
import java.io.IOException
import java.util.regex.Pattern
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request

open class RepoRelease {
    private val client = OkHttpClient()
    private val versionPattern =
        Pattern.compile("tag/(\\d{4}\\.\\d{2}\\.\\d{2})")
    protected val repo = ""
    protected val assetFileName = ""
    private val releaseUrl: String
        get() {
            return ""
        }

    fun getLatestVersion(): String? {
        val request = Request.Builder().url(releaseUrl).build()

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

    protected fun getReleaseDownloadUrl(version: String): String {
        return "https://github.com/$repo/releases/download/%s/$assetFileName"
    }

    fun downloadVersion(
        scope: CoroutineScope,
        context: Context,
        version: String,
        downloadCb: (Float) -> Unit = {}
    ): Job {
        val downloadUrl = getReleaseDownloadUrl(version)
        val outputFile = File(context.filesDir, YT_GET_FILE_NAME)
        val session =
            HttpDownloadSession(
                url = downloadUrl,
                path = outputFile.absolutePath
            )
        return scope.launch(Dispatchers.IO) { session.start() }
    }

    /**
     * Compares versions.
     *
     * @return 1: version1 > version2, 0: equal, -1: version1 < version2
     */
    fun compareVersions(version1: String, version2: String): Int {
        val v1 =
            version1.split(".").map {
                try {
                    it.toInt()
                } catch (e: NumberFormatException) {
                    it.replace("\\D*$".toRegex(), "").toIntOrNull() ?: 0
                }
            }
        val v2 =
            version2.split(".").map {
                try {
                    it.toInt()
                } catch (e: NumberFormatException) {
                    it.replace("\\D*$".toRegex(), "").toIntOrNull() ?: 0
                }
            }

        for (i in 0 until maxOf(v1.size, v2.size)) {
            val num1 = v1.getOrElse(i) { 0 }
            val num2 = v2.getOrElse(i) { 0 }

            when {
                num1 > num2 -> return 1
                num1 < num2 -> return -1
            }
        }
        return 0
    }
}
