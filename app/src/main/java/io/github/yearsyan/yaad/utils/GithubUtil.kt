package io.github.yearsyan.yaad.utils

import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.regex.Pattern

object GithubUtil {

    private val client = OkHttpClient()

    suspend fun getVersionList(repo: String): List<String> {
        val request = Request.Builder()
            .url("https://api.github.com/repos/$repo/releases")
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return emptyList()

                val body = response.body?.string() ?: return emptyList()
                val versions = mutableListOf<String>()
                val pattern = Pattern.compile("\"tag_name\":\"(\\d{4}\\.\\d{2}\\.\\d{2})\"")

                val matcher = pattern.matcher(body)
                while (matcher.find()) {
                    matcher.group(1)?.let { versions.add(it) }
                }
                versions.sortedWith { v1, v2 -> -compareVersions(v1, v2) } // Sort from new to old
            }
        } catch (e: IOException) {
            emptyList()
        }
    }

    /**
     * Compares versions.
     * @return 1: version1 > version2, 0: 相等, -1: version1 < version2
     */
    fun compareVersions(version1: String, version2: String): Int {
        val v1 = version1.split(".").map {
            try {
                it.toInt()
            } catch (e: NumberFormatException) {
                it.replace("\\D*$".toRegex(), "").toIntOrNull() ?: 0
            }
        }
        val v2 = version2.split(".").map {
            try {
                it.toInt()
            } catch (e: NumberFormatException) { it.replace("\\D*$".toRegex(), "").toIntOrNull() ?: 0 }
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