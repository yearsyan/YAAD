package io.github.yearsyan.yaad.utils

import android.content.Context
import io.github.yaad.downloader_core.HttpDownloadSession
import java.io.File
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

open class RepoRelease {
    private val client = OkHttpClient()
    open val repo = ""
    open val assetFileName = ""
    private val releaseUrl: String
        get() {
            return "https://api.github.com/repos/${repo}/releases/latest"
        }

    fun getLatestVersion(): String? {
        val request =
            Request.Builder()
                .url(releaseUrl)
                .header("Accept", "application/vnd.github.v3+json")
                .build()

        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null

                val body = response.body?.string() ?: return null
                val jsonObject = JSONObject(body)
                jsonObject
                    .optString("tag_name", null.toString())
                    .replaceFirst("^v".toRegex(), "")
            }
        } catch (e: IOException) {
            null
        }
    }

    open fun getReleaseDownloadUrl(version: String): String {
        return "https://github.com/$repo/releases/download/$version/$assetFileName"
    }

    fun downloadVersion(
        scope: CoroutineScope,
        context: Context,
        version: String,
        downloadCb: (Float) -> Unit = {}
    ): Job {
        val downloadUrl = getReleaseDownloadUrl(version)
        val outputFile = File(context.filesDir, assetFileName)
        val session =
            HttpDownloadSession(
                url = downloadUrl,
                path = outputFile.absolutePath
            )
        return scope.launch(Dispatchers.IO) { session.start() }
    }

    fun exist(context: Context): Boolean {
        val file = File(context.filesDir, assetFileName)
        return file.exists()
    }
}
