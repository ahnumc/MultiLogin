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
import java.net.URL
import java.net.URLClassLoader
import java.security.MessageDigest
import java.util.concurrent.atomic.AtomicBoolean

class PluginLoader(private val plugin: IPlugin) {
    private val librariesFolder = File(plugin.dataFolder, "libraries")
    private val loaded = AtomicBoolean(false)

    @Synchronized
    @Throws(Exception::class)
    fun load() {
        if (loaded.getAndSet(true)) throw UnsupportedOperationException("Repeated call.")
        removeAllFiles(plugin.tempFolder)
        generateFolder()

        val classLoader = PluginLoader::class.java.classLoader as URLClassLoader
        val addUrlMethod = URLClassLoader::class.java.getDeclaredMethod("addURL", URL::class.java)
        addUrlMethod.isAccessible = true

        val needDownload = libraries.filterNot { library ->
            tryLoadLibrary(library, addUrlMethod, classLoader)
        }

        if (needDownload.isNotEmpty()) {
            LoggerProvider.logger.info("Downloading ${needDownload.size} missing files...")
            val downloadFlows = ParallelFlows<Unit?>(
                needDownload.map { LibraryDownloadFlows(it, librariesFolder, plugin.tempFolder) }
            )
            if (downloadFlows.run(null) == Signal.TERMINATED) {
                throw InitialFailedException("Failed to download the missing file.")
            }
        }

        for (library in needDownload) {
            val file = File(librariesFolder, library.fileName)
            if (!tryLoadLibrary(library, addUrlMethod, classLoader)) {
                throw InitialFailedException(
                    "Failed to validate the digest value of the file ${file.absolutePath} that was just downloaded."
                )
            }
        }
    }

    @Synchronized
    @Throws(Exception::class)
    fun close() {
        removeAllFiles(plugin.tempFolder)
    }

    @Throws(Exception::class)
    private fun tryLoadLibrary(
        library: Library,
        addUrlMethod: java.lang.reflect.Method,
        classLoader: URLClassLoader
    ): Boolean {
        val file = File(librariesFolder, library.fileName)
        if (!file.exists() || file.length() == 0L) return false

        val sha256 = getSha256(file)
        LoggerProvider.logger.debug("The digest value of calculation file ${file.name} is $sha256.")
        if (sha256 != libraryDigestMap[library]) {
            LoggerProvider.logger.warn(
                "Failed to validate digest value of file ${file.absolutePath}, it will be re-downloaded."
            )
            return false
        }

        addUrlMethod.invoke(classLoader, file.toURI().toURL())
        return true
    }

    @Throws(Exception::class)
    private fun generateFolder() {
        ensureDirectory(librariesFolder)
        ensureDirectory(plugin.tempFolder)
    }

    @Throws(Exception::class)
    private fun getSha256(file: File): String {
        FileInputStream(file).use { fis ->
            val digest = MessageDigest.getInstance("SHA-256")
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val bytesRead = fis.read(buffer)
                if (bytesRead < 0) break
                digest.update(buffer, 0, bytesRead)
            }
            return digest.digest().joinToString("") { "%02x".format(it) }
        }
    }

    companion object {
        val libraryDigestMap: Map<Library, String> = loadResource(".digests") { lines ->
            lines.associate { line ->
                val parts = line.split("=", limit = 2)
                require(parts.size == 2) { "Invalid digest entry: $line" }
                Library.of(parts[0], ":") to parts[1]
            }
        }

        val libraries: Set<Library> = loadResource("libraries") { lines ->
            lines.map { Library.of(it, "\\s+") }.toSet()
        }

        val repositories: List<String> = loadResource("repositories") { lines ->
            lines.map { repository -> repository.takeIf { it.endsWith("/") } ?: "$repository/" }.toList()
        }

        init {
            libraries.forEach { library ->
                if (library !in libraryDigestMap) {
                    throw InitialFailedException("Missing digest for file ${library.fileName}.")
                }
            }
        }

        private fun ensureDirectory(folder: File) {
            if (!folder.exists() && !folder.mkdirs()) {
                throw Exception("Unable to create folder: ${folder.absolutePath}")
            }
        }

        private fun <T> loadResource(name: String, transform: (Sequence<String>) -> T): T {
            return try {
                requireNotNull(PluginLoader::class.java.classLoader.getResourceAsStream(name)) {
                    "Missing resource: $name"
                }.bufferedReader().use { reader ->
                    transform(reader.lineSequence().filterNot { line -> line.isBlank() || line.startsWith('#') })
                }
            } catch (e: Exception) {
                throw InitialFailedException(e)
            }
        }
    }
}
