package io.github.yearsyan.yaad.utils

import android.content.Context
import java.io.File
import java.io.FileOutputStream
import java.io.IOException


object AssetUtil {

    fun copyAssetIfNeeded(context: Context, fileName: String): Boolean {
        val targetFile = File(context.filesDir, fileName)
        if (targetFile.exists()) return false // Already copied

        try {
            context.assets.open(fileName).use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            }
        } catch (e: IOException) {
            return false
        }
        return true
    }

    fun getFilePath(context: Context, fileName: String): String {
        return File(context.filesDir, fileName).absolutePath
    }
}
