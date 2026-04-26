package moe.caa.multilogin.api.internal.util

import org.jetbrains.annotations.ApiStatus
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Files

/**
 * 流工具
 */
@ApiStatus.Internal
object IOUtil {
    /**
     * 拷贝流
     */
    @JvmStatic
    @Throws(IOException::class)
    fun copy(`is`: InputStream, os: OutputStream) {
        val buffer = ByteArray(1024)
        var n: Int
        while ((`is`.read(buffer).also { n = it }) != -1) {
            os.write(buffer, 0, n)
        }
        os.flush()
    }

    /**
     * 递归删除文件
     */
    @JvmStatic
    @Throws(IOException::class)
    fun removeAllFiles(file: File) {
        if (!file.exists()) return
        if (!file.isFile()) {
            file.listFiles()?.forEach { removeAllFiles(it) }
        }
        Files.delete(file.toPath())
    }
}
