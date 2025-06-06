package io.github.yaad.downloader_core

object NativeBridge {
    init {
        System.loadLibrary("downloader-core")
    }

    external fun openFile(path: String): Int

    external fun resizeFile(fd: Int, size: Long): Int

    external fun mmapFile(fd: Int, size: Long): Long

    external fun writeByte(ptr: Long, offset: Long, value: Byte)

    external fun writeByteArray(ptr: Long, offset: Long, array: ByteArray, srcOffset: Int, srcLength: Int)

    external fun closeFile(fd: Int)

    external fun msync(ptr: Long, size: Long)

    external fun munmap(ptr: Long, size: Long)

    external fun getSystemPageSize(): Int
}
