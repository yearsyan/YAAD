package io.github.yaad.downloader_core

import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

object FileHashUtils {

    enum class HashType(val algorithm: String) {
        MD5("MD5"),
        SHA1("SHA-1"),
        SHA256("SHA-256"),
        SHA512("SHA-512")
    }

    fun calculateHash(file: File, type: HashType): String {
        val buffer = ByteArray(1024 * 8)
        val digest = MessageDigest.getInstance(type.algorithm)

        FileInputStream(file).use { fis ->
            var read = fis.read(buffer)
            while (read != -1) {
                digest.update(buffer, 0, read)
                read = fis.read(buffer)
            }
        }

        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    fun calculateAllHashes(file: File): Map<HashType, String> {
        val results = mutableMapOf<HashType, String>()
        for (type in HashType.entries) {
            results[type] = calculateHash(file, type)
        }
        return results
    }
}
