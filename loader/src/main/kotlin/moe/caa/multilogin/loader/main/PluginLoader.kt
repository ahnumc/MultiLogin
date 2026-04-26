package moe.caa.multilogin.loader.main

import moe.caa.multilogin.api.internal.logger.LoggerProvider
import moe.caa.multilogin.api.internal.plugin.IPlugin
import moe.caa.multilogin.api.internal.util.IOUtil.removeAllFiles
import moe.caa.multilogin.flows.workflows.ParallelFlows
import moe.caa.multilogin.flows.workflows.Signal
import moe.caa.multilogin.loader.exception.InitialFailedException
import moe.caa.multilogin.loader.library.Library
import moe.caa.multilogin.loader.task.LibraryDownloadFlows
import java.io.File
import java.io.FileInputStream
import java.io.ByteArrayOutputStream
import java.net.URL
import java.net.URLClassLoader
import java.security.MessageDigest
import java.util.concurrent.atomic.AtomicBoolean

class PluginLoader(plugin: IPlugin) {
    private val librariesFolder: File
    private val plugin: IPlugin
    private val loaded = AtomicBoolean(false)

    init {
        this.plugin = plugin
        this.librariesFolder = File(plugin.dataFolder, "libraries")
    }

    @Synchronized
    @Throws(Exception::class)
    fun load() {
        if (loaded.getAndSet(true)) throw UnsupportedOperationException("Repeated call.")
        removeAllFiles(plugin.tempFolder)
        generateFolder()

        val classLoader = PluginLoader::class.java.classLoader as URLClassLoader
        val addUrlMethod = URLClassLoader::class.java.getDeclaredMethod("addURL", URL::class.java)
        addUrlMethod.isAccessible = true

        val needDownload = mutableListOf<Library>()
        for (library in libraries) {
            val file = File(librariesFolder, library.fileName)
            if (file.exists() && file.length() != 0L) {
                val sha256 = getSha256(file)
                LoggerProvider.logger.debug("The digest value of calculation file ${file.name} is $sha256.")
                if (sha256 == libraryDigestMap[library]) {
                    addUrlMethod.invoke(classLoader, file.toURI().toURL())
                    continue
                }
                LoggerProvider.logger.warn("Failed to validate digest value of file ${file.absolutePath}, it will be re-downloaded.")
            }
            needDownload.add(library)
        }

        if (needDownload.isNotEmpty()) {
            LoggerProvider.logger.info("Downloading ${needDownload.size} missing files...")
            val downloadFlows = ParallelFlows<Void?>(
                needDownload.map { LibraryDownloadFlows(it, librariesFolder, plugin.tempFolder) }
            )
            if (downloadFlows.run(null) == Signal.TERMINATED) {
                throw InitialFailedException("Failed to download the missing file.")
            }
        }

        for (library in needDownload) {
            val file = File(librariesFolder, library.fileName)
            val sha256 = getSha256(file)
            LoggerProvider.logger.debug("The digest value of calculation file ${file.name} is $sha256.")
            if (sha256 == libraryDigestMap[library]) {
                addUrlMethod.invoke(classLoader, file.toURI().toURL())
                continue
            }
            throw InitialFailedException("Failed to validate the digest value of the file ${file.absolutePath} that was just downloaded.")
        }
    }

    @Synchronized
    @Throws(Exception::class)
    fun close() {
        removeAllFiles(plugin.tempFolder)
    }

    @Throws(Exception::class)
    private fun generateFolder() {
        if (!librariesFolder.exists() && !librariesFolder.mkdirs())
            throw Exception("Unable to create folder: ${librariesFolder.absolutePath}")
        if (!plugin.tempFolder!!.exists() && !plugin.tempFolder!!.mkdirs())
            throw Exception("Unable to create folder: ${plugin.tempFolder!!.absolutePath}")
    }

    @Throws(Exception::class)
    private fun getSha256(file: File): String {
        FileInputStream(file).use { fis ->
            ByteArrayOutputStream().use { baos ->
                val buff = ByteArray(1024)
                var n: Int
                while (fis.read(buff).also { n = it } > 0) baos.write(buff, 0, n)
                return MessageDigest.getInstance("SHA-256").digest(baos.toByteArray())
                    .joinToString("") { "%02x".format(it) }
            }
        }
    }

    companion object {
        val libraryDigestMap: Map<Library?, String?>
        val libraries: Set<Library>
        val repositories: List<String?>

        init {
            try {
                libraryDigestMap =
                    PluginLoader::class.java.classLoader.getResourceAsStream(".digests")!!.bufferedReader()
                        .use { reader ->
                            reader.lineSequence()
                                .filter { it.isNotBlank() && it[0] != '#' }
                                .associate { line ->
                                    val parts = line.split("=")
                                    Library.of(parts[0], ":") to parts[1]
                                }
                        }
            } catch (e: Exception) {
                throw InitialFailedException(e)
            }

            try {
                libraries = PluginLoader::class.java.classLoader.getResourceAsStream("libraries")!!.bufferedReader()
                    .use { reader ->
                        reader.lineSequence()
                            .filter { it.isNotBlank() && it[0] != '#' }
                            .map { Library.of(it, "\\s+") }
                            .toSet()
                    }
            } catch (e: Exception) {
                throw InitialFailedException(e)
            }

            try {
                repositories =
                    PluginLoader::class.java.classLoader.getResourceAsStream("repositories")!!.bufferedReader()
                        .use { reader ->
                            reader.lineSequence()
                                .filter { it.isNotBlank() && it[0] != '#' }
                                .map { if (it.endsWith("/")) it else "$it/" }
                                .toList()
                        }
            } catch (e: Exception) {
                throw InitialFailedException(e)
            }

            for (library in libraries) {
                if (library !in libraryDigestMap) {
                    throw InitialFailedException("Missing digest for file ${library.fileName}.")
                }
            }
        }
    }
}
