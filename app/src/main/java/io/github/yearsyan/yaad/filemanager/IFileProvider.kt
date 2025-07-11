package io.github.yearsyan.yaad.filemanager

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.ui.graphics.vector.ImageVector

enum class IconType {
    RES_ID,
    BITMAP,
    IMAGE_VECTOR,
    FETCHER,
    DEFAULT
}

interface IFileNodeProvider {
    val isDirectory: Boolean
    val name: String
    val path: String
    val subTitle: String
    val iconType: IconType
        get() = IconType.DEFAULT

    val uri: Uri?

    val fileSize: Long

    fun listFiles(): List<IFileNodeProvider>

    fun canRead(): Boolean

    fun canWrite(): Boolean

    fun canRandomAccess(): Boolean

    fun createTime(): Long

    fun getRaw(): Object

    fun getImageVectorIcon(): ImageVector

    fun getBitmapIcon(): Bitmap

    fun getResIdIcon(): Int

    fun rename(name: String)

    suspend fun fetchIcon(): Bitmap? {
        return null
    }
}

interface IFileProvider<T> {
    fun requestCreate(data: T, onResult: (IFileNodeProvider) -> Unit)
}
