package io.github.yearsyan.yaad.filemanager

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileCopy
import androidx.compose.material.icons.filled.Folder
import androidx.compose.ui.graphics.vector.ImageVector
import io.github.yearsyan.yaad.utils.FileUtils
import io.github.yearsyan.yaad.utils.scaleBitmapToFit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class LocalFileProvider(private val file: File) : IFileNodeProvider {
    override val isDirectory: Boolean
        get() = file.isDirectory

    override val name: String
        get() = file.name

    override val path: String
        get() = file.path

    override val subTitle: String
        get() = ""

    override val iconType: IconType
        get() {
            if (FileUtils.isImageFile(name)) {
                return IconType.FETCHER
            }
            return IconType.IMAGE_VECTOR
        }

    override val uri: Uri
        get() = Uri.fromFile(file)

    override val fileSize: Long
        get() = file.length()


    override fun listFiles(): List<IFileNodeProvider> {
        val fileList = file.listFiles()
        if (fileList == null) {
            throw RuntimeException("list file error")
        }
        return fileList.map { LocalFileProvider(it) }
    }

    override fun canRead(): Boolean {
        return file.canRead()
    }

    override fun canWrite(): Boolean {
        return file.canWrite()
    }

    override fun canRandomAccess(): Boolean {
        return true
    }

    override fun createTime(): Long {
        return 0
    }

    override fun getRaw(): Object {
        return file as Object
    }

    override fun getImageVectorIcon(): ImageVector {
        return if (file.isDirectory) {
            Icons.Default.Folder
        } else {
            Icons.Default.FileCopy
        }
    }

    override fun getBitmapIcon(): Bitmap {
        throw RuntimeException("Icon Type Error")
    }

    override fun getResIdIcon(): Int {
        throw RuntimeException("Icon Type Error")
    }

    override fun rename(name: String) {
        file.renameTo(File(file.parentFile, name))
    }

    override suspend fun fetchIcon(): Bitmap? {
        return withContext(Dispatchers.Default) {
            return@withContext FileUtils.fileToBitmap(file)?.let {
                scaleBitmapToFit(it, 150, 150)
            }
        }
        return null
    }
}
