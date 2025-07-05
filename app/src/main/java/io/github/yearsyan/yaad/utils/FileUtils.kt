package io.github.yearsyan.yaad.utils

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.RequiresApi
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object FileUtils {

    /**
     * 将文件移动到下载目录，兼容Android 10+的作用域存储
     *
     * @param context 上下文
     * @param sourceFile 源文件
     * @param fileName 目标文件名
     * @return 成功返回true，失败返回false
     */
    suspend fun moveToDownloads(
        context: Context,
        sourceFile: File,
        fileName: String
    ): Boolean =
        withContext(Dispatchers.IO) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // Android 10+ 使用MediaStore API
                    moveToDownloadsWithMediaStore(context, sourceFile, fileName)
                } else {
                    if (!PermissionUtils.hasStoragePermission(context)) {
                        return@withContext false
                    }
                    // Android 9及以下使用传统方式
                    moveToDownloadsLegacy(sourceFile, fileName)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }

    /** Android 10+使用MediaStore API移动文件到下载目录 */
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun moveToDownloadsWithMediaStore(
        context: Context,
        sourceFile: File,
        fileName: String
    ): Boolean {
        if (!sourceFile.exists()) {
            return false
        }

        val resolver: ContentResolver = context.contentResolver

        // 创建ContentValues
        val contentValues =
            ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, getMimeType(fileName))
                put(
                    MediaStore.MediaColumns.RELATIVE_PATH,
                    Environment.DIRECTORY_DOWNLOADS
                )
            }

        return try {
            // 插入到MediaStore
            val uri: Uri? =
                resolver.insert(
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                    contentValues
                )
            if (uri == null) {
                Log.e("FileUtils", "url is null")
            }
            uri?.let { targetUri ->
                // 复制文件内容
                resolver.openOutputStream(targetUri)?.use { outputStream ->
                    FileInputStream(sourceFile).use { inputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }

                // 删除源文件
                sourceFile.delete()
                true
            } == true
        } catch (e: IOException) {
            e.printStackTrace()
            false
        }
    }

    /** Android 9及以下使用传统方式移动文件 */
    private fun moveToDownloadsLegacy(
        sourceFile: File,
        fileName: String
    ): Boolean {
        if (!sourceFile.exists()) {
            return false
        }

        val downloadsDir =
            Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS
            )
        if (!downloadsDir.exists()) {
            downloadsDir.mkdirs()
        }

        val targetFile = File(downloadsDir, fileName)

        return try {
            sourceFile.copyTo(targetFile, overwrite = true)
            sourceFile.delete()
            true
        } catch (e: IOException) {
            e.printStackTrace()
            false
        }
    }

    /** 根据文件扩展名获取MIME类型 */
    private fun getMimeType(fileName: String): String {
        return when (fileName.substringAfterLast('.', "").lowercase()) {
            "mp4" -> "video/mp4"
            "avi" -> "video/x-msvideo"
            "mkv" -> "video/x-matroska"
            "mov" -> "video/quicktime"
            "wmv" -> "video/x-ms-wmv"
            "flv" -> "video/x-flv"
            "webm" -> "video/webm"
            "m4v" -> "video/x-m4v"
            "3gp" -> "video/3gpp"
            "ts" -> "video/mp2t"
            else -> "video/*"
        }
    }

    public fun isVideoFile(fileName: String): Boolean {
        return when (fileName.substringAfterLast('.', "").lowercase()) {
            "mp4",
            "avi",
            "mkv",
            "mov",
            "wmv",
            "flv",
            "webm",
            "m4v",
            "3gp",
            "ts" -> true
            else -> false
        }
    }

    fun isImageFile(fileName: String): Boolean {
        return when (fileName.substringAfterLast('.', "").lowercase()) {
            "jpg",
            "jpeg",
            "png",
            "webp",
            "gif" -> true
            else -> false
        }
    }

    fun fileToBitmap(file: File): Bitmap? {
        return try {
            FileInputStream(file).use { inputStream ->
                BitmapFactory.decodeStream(inputStream)
            }
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }
}
