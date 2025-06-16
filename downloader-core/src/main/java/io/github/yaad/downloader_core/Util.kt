package io.github.yaad.downloader_core

import android.annotation.SuppressLint
import android.webkit.WebSettings

fun getFileName(url: String, defaultValue: String): String {
    return try {
        val uri = java.net.URI(url)
        val path = uri.path ?: return defaultValue
        val fileName = path.substringAfterLast('/', missingDelimiterValue = "")

        fileName.ifEmpty { defaultValue }
    } catch (e: Exception) {
        defaultValue
    }
}

@SuppressLint("PrivateApi")
fun getAppContext(): android.content.Context? {
    return try {
        val applicationClass = Class.forName("android.app.ActivityThread")
        val method = applicationClass.getMethod("currentApplication")
        val application = method.invoke(null) as android.app.Application
        application.applicationContext
    } catch (e: Exception) {
        null
    }
}

fun getSystemUserAgent(): String {
    return try {
        WebSettings.getDefaultUserAgent(getAppContext())
    } catch (e: Exception) {
        "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/113.0.0.0 Mobile Safari/537.36"
    }
}
