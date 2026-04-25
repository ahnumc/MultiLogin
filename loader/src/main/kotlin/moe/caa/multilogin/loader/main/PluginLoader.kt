package moe.caa.multilogin.loader.main

import moe.caa.multilogin.api.internal.logger.LoggerProvider
import moe.caa.multilogin.api.internal.main.MultiCoreAPI
import moe.caa.multilogin.api.internal.plugin.IPlugin
import moe.caa.multilogin.api.internal.util.IOUtil.copy
import moe.caa.multilogin.api.internal.util.IOUtil.removeAllFiles
import moe.caa.multilogin.flows.workflows.ParallelFlows
import moe.caa.multilogin.flows.workflows.Signal
import moe.caa.multilogin.loader.classloader.IExtURLClassLoader
import moe.caa.multilogin.loader.classloader.PriorAllURLClassLoader
import moe.caa.multilogin.loader.exception.InitialFailedException
import moe.caa.multilogin.loader.library.Library
import moe.caa.multilogin.loader.task.LibraryDownloadFlows
import java.io.*
import java.nio.file.Files
import java.security.MessageDigest
import java.util.concurrent.atomic.AtomicBoolean

class PluginLoader(plugin: IPlugin) {
    private val librariesFolder: File
    private val plugin: IPlugin?
    private val loaded = AtomicBoolean(false)
    var pluginClassLoader: IExtURLClassLoader? = PriorAllURLClassLoader(
        arrayOfNulls(0), PluginLoader::class.java.classLoader,
        mutableSetOf("moe.caa.multilogin.", "java.", "net.minecraft.", "com.mojang.", "org.bukkit.")
    )
        private set

    var coreObject: MultiCoreAPI? = null
        private set

    init {
        this.plugin = plugin
        this.librariesFolder = File(plugin.dataFolder, "libraries")
    }

    @Synchronized
    @Throws(Exception::class)
    fun load(vararg additions: String?) {
        if (loaded.getAndSet(true)) throw UnsupportedOperationException("Repeated call.")
        removeAllFiles(plugin!!.tempFolder)
        generateFolder()

        val needDownload = mutableListOf<Library>()
        for (library in libraries) {
            val file = File(librariesFolder, library.fileName)
            if (file.exists() && file.length() != 0L) {
                val sha256 = getSha256(file)
                LoggerProvider.logger.debug("The digest value of calculation file ${file.name} is $sha256.")
                if (sha256 == libraryDigestMap[library]) {
                    pluginClassLoader!!.addURL(file.toURI().toURL())
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
                pluginClassLoader!!.addURL(file.toURI().toURL())
                continue
            }
            throw InitialFailedException("Failed to validate the digest value of the file ${file.absolutePath} that was just downloaded.")
        }

        loadNestJar(nestJarName, pluginClassLoader!!)
        for (addition in additions) loadNestJar(addition, pluginClassLoader!!)
        loadCore()
    }

    @Throws(IOException::class)
    private fun loadNestJar(nestJarName: String?, classLoader: IExtURLClassLoader) {
        val output = File.createTempFile("$nestJarName.", ".jar", plugin!!.tempFolder)
        if (!output.exists()) Files.createFile(output.toPath())
        output.deleteOnExit()
        PluginLoader::class.java.classLoader.getResourceAsStream(nestJarName).use { `is` ->
            FileOutputStream(output).use { fos ->
                copy(`is` ?: throw IOException("Resource not found: $nestJarName"), fos)
            }
        }
        classLoader.addURL(output.toURI().toURL())
    }

    @Throws(Exception::class)
    private fun loadCore() {
        val coreClass = findClass(coreClassName)
        for (constructor in coreClass.declaredConstructors) {
            val parameterTypes = constructor.parameterTypes
            if (parameterTypes.size == 1 && parameterTypes[0] == IPlugin::class.java) {
                coreObject = constructor.newInstance(plugin) as MultiCoreAPI
                return
            }
        }
        throw RuntimeException("Not found constructor")
    }

    @Throws(ClassNotFoundException::class)
    fun findClass(name: String?): Class<*> = Class.forName(name, true, pluginClassLoader!!.self())

    @Synchronized
    @Throws(Exception::class)
    fun close() {
        pluginClassLoader?.self()?.close()
        plugin!!.runServer!!.scheduler!!.shutdown()
        coreObject = null
        pluginClassLoader = null
        removeAllFiles(plugin.tempFolder)
    }

    @Throws(IOException::class)
    private fun generateFolder() {
        if (!librariesFolder.exists() && !librariesFolder.mkdirs())
            throw IOException("Unable to create folder: ${librariesFolder.absolutePath}")
        if (!plugin!!.tempFolder!!.exists() && !plugin.tempFolder!!.mkdirs())
            throw IOException("Unable to create folder: ${plugin.tempFolder!!.absolutePath}")
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
        const val nestJarName: String = "MultiLogin-Core.JarFile"
        const val coreClassName: String = "moe.caa.multilogin.core.main.MultiCore"

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
