package moe.caa.multilogin.loader.task

import moe.caa.multilogin.api.internal.logger.LoggerProvider
import moe.caa.multilogin.api.internal.util.IOUtil.copy
import moe.caa.multilogin.flows.workflows.BaseFlows
import moe.caa.multilogin.flows.workflows.Signal
import moe.caa.multilogin.loader.exception.InitialFailedException
import moe.caa.multilogin.loader.library.Library
import moe.caa.multilogin.loader.main.PluginLoader
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.nio.file.Files

/**
 * 表示文件依赖下载流
 */
class LibraryDownloadFlows(
    private val library: Library,
    private val librariesFolder: File,
    private val tempLibrariesFolder: File
) : BaseFlows<Unit?>() {
    override fun run(unused: Unit?): Signal {
        val output = File(librariesFolder, library.fileName)
        val tmp = File(tempLibrariesFolder, library.fileName)
        val exceptions = mutableListOf<Exception>()
        val bytes = PluginLoader.repositories.firstNotNullOfOrNull { repository ->
            val downloadUrl = repository + library.downloadUrl
            LoggerProvider.logger.debug("Downloading from $downloadUrl")
            try {
                getBytes(URL(downloadUrl))
            } catch (t: Exception) {
                val cause = "Download from %s failed.".format(downloadUrl)
                exceptions += InitialFailedException(cause, t)
                null
            }
        } ?: run {
            val cause = "Unable to download file %s.".format(library.fileName)
            exceptions.forEach { exception ->
                LoggerProvider.logger.error(InitialFailedException(cause, exception))
            }
            return Signal.TERMINATED
        }

        try {
            Files.deleteIfExists(tmp.toPath())
            Files.deleteIfExists(output.toPath())
            Files.write(tmp.toPath(), bytes)
            Files.move(tmp.toPath(), output.toPath())
            LoggerProvider.logger.info("Downloaded ${output.name}")
        } catch (t: Throwable) {
            LoggerProvider.logger.error("Unable to process file ${library.fileName}", t)
            return Signal.TERMINATED
        }

        return Signal.PASSED
    }

    companion object {
        @Throws(IOException::class)
        private fun getBytes(url: URL): ByteArray {
            val httpURLConnection = url.openConnection() as HttpURLConnection
            return try {
                httpURLConnection.doInput = true
                httpURLConnection.doOutput = false
                httpURLConnection.connectTimeout = 10000
                httpURLConnection.connect()

                if (httpURLConnection.responseCode == HttpURLConnection.HTTP_OK) {
                    httpURLConnection.inputStream.use { input ->
                        ByteArrayOutputStream().use { output ->
                            copy(input, output)
                            output.toByteArray()
                        }
                    }
                }
                throw IOException(httpURLConnection.responseCode.toString())
            } finally {
                httpURLConnection.disconnect()
            }
        }
    }
}
