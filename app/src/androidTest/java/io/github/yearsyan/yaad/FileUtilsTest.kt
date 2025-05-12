package io.github.yearsyan.yaad

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.yaad.downloader_core.FileHashUtils
import java.io.File
import java.io.FileOutputStream
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FileUtilsTest {
    @Test
    fun testHash() {
        val appContext =
            InstrumentationRegistry.getInstrumentation().targetContext
        val file = File(appContext.cacheDir, "test.txt")
        FileOutputStream(file).use { it.write("test\n".toByteArray()) }
        val hashes = FileHashUtils.calculateAllHashes(file)
        assertEquals(
            "d8e8fca2dc0f896fd7cb4cb0031ba249",
            hashes[FileHashUtils.HashType.MD5]
        )
        assertEquals(
            "4e1243bd22c66e76c2ba9eddc1f91394e57f9f83",
            hashes[FileHashUtils.HashType.SHA1]
        )
        assertEquals(
            "f2ca1bb6c7e907d06dafe4687e579fce76b37e4e93b7605022da52e6ccc26fd2",
            hashes[FileHashUtils.HashType.SHA256]
        )
        assertEquals(
            "0e3e75234abc68f4378a86b3f4b32a198ba301845b0cd6e50106e874345700cc6663a86c1ea125dc5e92be17c98f9a0f85ca9d5f595db2012f7cc3571945c123",
            hashes[FileHashUtils.HashType.SHA512]
        )
    }
}
