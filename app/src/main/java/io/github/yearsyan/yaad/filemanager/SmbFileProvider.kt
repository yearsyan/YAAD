package io.github.yearsyan.yaad.filemanager

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileCopy
import androidx.compose.material.icons.filled.Folder
import androidx.compose.ui.graphics.vector.ImageVector
import io.github.yearsyan.yaad.utils.FileUtils
import io.github.yearsyan.yaad.utils.scaleBitmapToFit
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import jcifs.CIFSContext
import jcifs.smb.SmbFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.Inet6Address
import java.net.InetAddress

fun getSmbUrlFromAddresses(addresses: List<InetAddress>, port: Int): String? {
    for (addressItem in addresses) {
        Log.d("getSmbUrlFromAddresses", "addr: ${addressItem.hostAddress}")
        val addressString = if (addressItem is Inet6Address)  {
            "[${addressItem.hostAddress}]"
        } else {
            addressItem.hostAddress
        }

        val baseUrl = "smb://$addressString"
        if (port != 445) {
            return "$baseUrl:$port/"
        }
        return "$baseUrl/"
    }
    return null
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
    private val isDir: Boolean = file.isDirectory

    override val iconType: IconType
        get() = iconTypeInternal

    override val uri: Uri
        get() = Uri.parse(file.url.toString())

    override val fileSize: Long
        get() = sizeInternal

    override val isDirectory: Boolean
        get() = isDir

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
        var t = IconType.IMAGE_VECTOR
        if (FileUtils.isImageFile(n)) {
            t = IconType.FETCHER
        }
        iconTypeInternal = t
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
            .filter { it.name != "IPC$/" } // fixme
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

    override suspend fun fetchIcon(): Bitmap? {
        val data = withContext(Dispatchers.IO) {
            try {
                return@withContext file.openInputStream().readAllBytes()
            } catch (e: Exception) {
                return@withContext null
            }
        }
        return data?.let {
            withContext(Dispatchers.Default) {
                return@withContext BitmapFactory.decodeByteArray(it, 0, it.size)?.let { res ->
                    scaleBitmapToFit(res, 150, 150)
                }
            }
        }
    }
}
