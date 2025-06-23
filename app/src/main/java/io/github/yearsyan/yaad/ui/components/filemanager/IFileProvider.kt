package io.github.yearsyan.yaad.ui.components.filemanager

import android.graphics.Bitmap
import androidx.compose.ui.graphics.vector.ImageVector

enum class IconType {
    RES_ID,
    BITMAP,
    IMAGE_VECTOR,
    DEFAULT
}

interface IFileNodeProvider {
    val isDirectory: Boolean
    val name: String
    val path: String
    val subTitle: String
    val iconType: IconType
        get() = IconType.DEFAULT

    fun listFiles(): List<IFileNodeProvider>

    fun canRead(): Boolean

    fun canWrite(): Boolean

    fun canRandomAccess(): Boolean

    fun createTime(): Long

    fun getRaw(): Object

    fun getImageVectorIcon(): ImageVector

    fun getBitmapIcon(): Bitmap

    fun getResIdIcon(): Int
}

interface IFileProvider {
    fun createByLink(url: String): IFileNodeProvider
}
