package io.github.yearsyan.yaad.utils

import android.content.Context
import io.github.yaad.downloader_core.HttpDownloadSession
import java.io.File
import java.io.IOException
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.json.JSONObject

open class RepoRelease {
    private val client = HttpClient(CIO)
    open val repo = ""
    open val assetFileName = ""
    private val releaseUrl: String
        get() {
            return "https://api.github.com/repos/${repo}/releases/latest"
        }

    fun getLatestVersion(): String? = runBlocking {
        try {
            val response = client.get(releaseUrl) {
                header("Accept", "application/vnd.github.v3+json")
            }

            if (response.status.value in 200..299) {
                val body = response.bodyAsText()
                val jsonObject = JSONObject(body)
                jsonObject.optString("tag_name", null.toString())
                    .replaceFirst("^v".toRegex(), "")
            } else {
                null
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
    ): Job {
        val downloadUrl = getReleaseDownloadUrl(version)
        val outputFile = File(context.filesDir, assetFileName).absolutePath
        val session = HttpDownloadSession(downloadUrl, outputFile)
        return scope.launch(Dispatchers.IO) { session.start() }
    }

    fun exist(context: Context): Boolean {
        val file = File(context.filesDir, assetFileName)
        return file.exists()
    }
}
