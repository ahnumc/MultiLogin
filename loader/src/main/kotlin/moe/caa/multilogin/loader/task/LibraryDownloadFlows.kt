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
import java.io.FileWriter
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.nio.file.Files

/**
 * 表示文件依赖下载流
 */
class LibraryDownloadFlows(
    private val library: Library?,
    private val librariesFolder: File?,
    private val tempLibrariesFolder: File?
) : BaseFlows<Void?>() {
    override fun run(unused: Void?): Signal {
        val output = File(librariesFolder, library!!.fileName)
        val tmp = File(tempLibrariesFolder, library.fileName)
        var bytes: ByteArray? = null

        val exceptions: MutableList<Exception?> = ArrayList()
        for (repository in PluginLoader.repositories) {
            val downloadUrl: String = repository + library.downloadUrl
            LoggerProvider.logger.debug("Downloading from $downloadUrl")
            try {
                bytes = getBytes(URL(downloadUrl))
                break
            } catch (t: Exception) {
                val cause = "Download from %s failed.".format(downloadUrl)
                exceptions.add(InitialFailedException(cause, t))
            }
        }

        if (bytes == null) {
            val cause = "Unable to download file %s.".format(library.fileName)
            exceptions.forEach { e: Exception? ->
                LoggerProvider.logger.error(InitialFailedException(cause, e))
            }
            return Signal.TERMINATED
        }

        try {
            if (!tmp.exists()) {
                Files.createFile(tmp.toPath())
            } else {
                FileWriter(tmp).use { fw ->
                    fw.write("")
                    fw.flush()
                }
            }
            if (output.exists()) {
                Files.delete(output.toPath())
            }

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
            httpURLConnection.doInput = true
            httpURLConnection.doOutput = false
            httpURLConnection.connectTimeout = 10000
            httpURLConnection.connect()

            if (httpURLConnection.responseCode == 200) {
                httpURLConnection.inputStream.use { input ->
                    ByteArrayOutputStream().use { output ->
                        copy(input, output)
                        return output.toByteArray()
                    }
                }
            }
            throw IOException(httpURLConnection.responseCode.toString())
        }
    }
}
