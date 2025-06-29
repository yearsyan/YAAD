package io.github.yearsyan.yaad.filemanager

import android.graphics.Bitmap
import android.net.Uri
import android.net.nsd.NsdServiceInfo
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileCopy
import androidx.compose.material.icons.filled.Folder
import androidx.compose.ui.graphics.vector.ImageVector
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import jcifs.CIFSContext
import jcifs.smb.SmbFile

class SmbEntryProvider() : IFileProvider<NsdServiceInfo> {
    override fun requestCreate(
        data: NsdServiceInfo,
        onResult: (IFileNodeProvider) -> Unit
    ) {}
}

class SmbFileProvider(
    private val file: SmbFile,
    private val context: CIFSContext,
    private val isRoot: Boolean = false
) : IFileNodeProvider {

    private val realName: String
    private val subTitleInternal: String
    private val iconTypeInternal: IconType
    private val imageVectorIcon: ImageVector?
    private val bitmapIcon: Bitmap? = null
    private val sizeInternal: Long

    override val iconType: IconType
        get() = iconTypeInternal
    override val uri: Uri
        get() = Uri.parse(file.url.toString())

    override val fileSize: Long
        get() = sizeInternal

    override val isDirectory: Boolean
        get() = file.isDirectory

    override val name: String
        get() = realName

    override val path: String
        get() = file.path

    override val subTitle: String
        get() = subTitleInternal

    init {
        val n = file.name
        val isDir = file.isDirectory
        realName =
            if (isDir && n.endsWith("/")) {
                n.dropLast(1)
            } else {
                n
            }
        iconTypeInternal = IconType.IMAGE_VECTOR
        imageVectorIcon =
            if (isDir) {
                Icons.Default.Folder
            } else {
                Icons.Default.FileCopy
            }

        val date =
            LocalDateTime.ofInstant(
                Date(file.createTime()).toInstant(),
                ZoneId.systemDefault()
            )
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        subTitleInternal = date.format(formatter)
        sizeInternal = file.contentLengthLong
    }

    override fun listFiles(): List<IFileNodeProvider> {
        return file
            .listFiles()
            .filter { !(isRoot && it.name == "IPC$/") }
            .map { SmbFileProvider(it, context) }
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
        return file.createTime()
    }

    override fun getRaw(): Object {
        return file as Object
    }

    override fun getImageVectorIcon(): ImageVector {
        if (imageVectorIcon == null) {
            throw RuntimeException("Icon Type Error")
        }
        return imageVectorIcon
    }

    override fun getBitmapIcon(): Bitmap {
        if (bitmapIcon == null) {
            throw RuntimeException("Icon Type Error")
        }
        return bitmapIcon
    }

    override fun getResIdIcon(): Int {
        throw RuntimeException("Icon Type Error")
    }

    override fun rename(name: String) {
        file.renameTo(SmbFile("${file.parent}${name}", context))
    }
}
