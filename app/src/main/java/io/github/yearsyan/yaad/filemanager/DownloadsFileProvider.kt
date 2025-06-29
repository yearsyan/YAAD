package io.github.yearsyan.yaad.filemanager

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.FileCopy
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.ui.graphics.vector.ImageVector
import java.text.SimpleDateFormat
import java.util.*

class DownloadsFileProvider(
    private val context: Context,
    override val uri: Uri? = null,
    private val fileName: String? = null,
    private val _fileSize: Long = 0,
    private val dateAdded: Long = 0,
    private val mimeType: String? = null
) : IFileNodeProvider {

    companion object {
        private const val DOWNLOADS_FOLDER_NAME = "Downloads"
        
        fun createRootProvider(context: Context): DownloadsFileProvider {
            return DownloadsFileProvider(context)
        }
    }

    override val isDirectory: Boolean
        get() = uri == null // 根目录是文件夹

    override val name: String
        get() = fileName ?: DOWNLOADS_FOLDER_NAME

    override val path: String
        get() = uri?.toString() ?: "content://media/external/downloads"

    override val subTitle: String
        get() = if (dateAdded > 0) {
            val date = Date(dateAdded * 1000) // MediaStore 时间戳是秒
            val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            formatter.format(date)
        } else {
            ""
        }

    override val iconType: IconType
        get() = IconType.IMAGE_VECTOR

    override val fileSize: Long
        get() = _fileSize

    override fun listFiles(): List<IFileNodeProvider> {
        if (!isDirectory) {
            return emptyList()
        }

        val contentResolver: ContentResolver = context.contentResolver
        val downloadsUri = MediaStore.Downloads.EXTERNAL_CONTENT_URI
        
        val projection = arrayOf(
            MediaStore.Downloads._ID,
            MediaStore.Downloads.DISPLAY_NAME,
            MediaStore.Downloads.SIZE,
            MediaStore.Downloads.DATE_ADDED,
            MediaStore.Downloads.MIME_TYPE
        )
        
        val sortOrder = "${MediaStore.Downloads.DATE_ADDED} DESC"
        
        val cursor = try {
            contentResolver.query(
                downloadsUri,
                projection,
                null,
                null,
                sortOrder
            )
        } catch (e: SecurityException) {
            // 权限不足时返回空列表
            return emptyList()
        }

        val fileList = mutableListOf<IFileNodeProvider>()
        
        cursor?.use { 
            try {
                val idColumn = it.getColumnIndexOrThrow(MediaStore.Downloads._ID)
                val nameColumn = it.getColumnIndexOrThrow(MediaStore.Downloads.DISPLAY_NAME)
                val sizeColumn = it.getColumnIndexOrThrow(MediaStore.Downloads.SIZE)
                val dateAddedColumn = it.getColumnIndexOrThrow(MediaStore.Downloads.DATE_ADDED)
                val mimeTypeColumn = it.getColumnIndexOrThrow(MediaStore.Downloads.MIME_TYPE)
                
                while (it.moveToNext()) {
                    val id = it.getLong(idColumn)
                    val name = it.getString(nameColumn)
                    val size = it.getLong(sizeColumn)
                    val dateAdded = it.getLong(dateAddedColumn)
                    val mimeType = it.getString(mimeTypeColumn)
                    
                    val fileUri = Uri.withAppendedPath(downloadsUri, id.toString())
                    
                    fileList.add(
                        DownloadsFileProvider(
                            context = context,
                            uri = fileUri,
                            fileName = name,
                            _fileSize = size,
                            dateAdded = dateAdded,
                            mimeType = mimeType
                        )
                    )
                }
            } catch (e: Exception) {
                // 处理查询过程中的异常
                e.printStackTrace()
            }
        }
        
        return fileList
    }

    override fun canRead(): Boolean {
        return true // MediaStore 通常可读
    }

    override fun canWrite(): Boolean {
        return false // MediaStore 通常不可写，需要通过 ContentResolver
    }

    override fun canRandomAccess(): Boolean {
        return true
    }

    override fun createTime(): Long {
        return dateAdded * 1000 // 转换为毫秒
    }

    override fun getRaw(): Object {
        return uri as Object
    }

    override fun getImageVectorIcon(): ImageVector {
        return if (isDirectory) {
            Icons.Default.Folder
        } else {
            // 根据 MIME 类型返回不同的图标
            when {
                mimeType?.startsWith("image/") == true -> Icons.Default.Image
                mimeType?.startsWith("video/") == true -> Icons.Default.VideoFile
                mimeType?.startsWith("audio/") == true -> Icons.Default.AudioFile
                else -> Icons.Default.FileCopy
            }
        }
    }

    override fun getBitmapIcon(): Bitmap {
        throw RuntimeException("Icon Type Error")
    }

    override fun getResIdIcon(): Int {
        throw RuntimeException("Icon Type Error")
    }

    override fun rename(name: String) {
        // MediaStore 不支持直接重命名，需要通过 ContentResolver 更新
        // 这里暂时抛出异常，实际使用时需要实现 ContentResolver 的更新逻辑
        throw UnsupportedOperationException("Rename not supported for MediaStore downloads")
    }
}

/*
注意事项：
1. 需要在 AndroidManifest.xml 中添加相应的权限
2. Android 13+ 需要 READ_MEDIA_* 权限
3. 需要处理权限请求和异常情况
4. MediaStore 操作是异步的，建议在后台线程中执行
*/ 