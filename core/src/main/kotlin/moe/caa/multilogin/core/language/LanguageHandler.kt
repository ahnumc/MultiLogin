package moe.caa.multilogin.core.language

import moe.caa.multilogin.api.internal.language.LanguageAPI
import moe.caa.multilogin.api.internal.logger.LoggerProvider
import moe.caa.multilogin.api.internal.util.IOUtil.copy
import moe.caa.multilogin.api.internal.util.ValueUtil.transPapi
import moe.caa.multilogin.core.main.MultiCore
import java.io.*
import java.nio.charset.StandardCharsets
import java.util.*

class LanguageHandler(private val core: MultiCore) : LanguageAPI {
    private lateinit var language: Properties

    @Throws(IOException::class)
    fun init() {
        reload()
    }

    override fun getMessage(node: String?, vararg pairs: Pair<String, Any?>): String {
        return transPapi(language.getProperty(node), *pairs)
    }

    @Throws(IOException::class)
    fun reload() {
        val bundled = Properties()
        (javaClass.getResourceAsStream("/message.properties")
            ?: error("Resource not found: /message.properties")).use { resourceAsStream ->
            bundled.load(InputStreamReader(resourceAsStream, StandardCharsets.UTF_8))
        }

        val messagePropertiesFile = File(core.plugin.dataFolder, "message.properties")
        if (!messagePropertiesFile.exists()) {
            FileOutputStream(messagePropertiesFile).use { outputStream ->
                (javaClass.getResourceAsStream("/message.properties")
                    ?: error("Resource not found: /message.properties")).use { copy(it, outputStream) }
            }
            LoggerProvider.logger.info("Extract: message.properties")
        }

        val disk = Properties()
        FileInputStream(messagePropertiesFile).use { inputStream ->
            disk.load(InputStreamReader(inputStream, StandardCharsets.UTF_8))
        }

        // bundled resource is source of truth; disk file provides overrides
        val tmp = Properties()
        for (entry in bundled.entries) {
            val key = entry.key.toString()
            val value = disk.getProperty(key)
            if (value != null) {
                tmp.setProperty(key, value)
            } else {
                tmp.setProperty(key, entry.value.toString())
                LoggerProvider.logger.warn("Missing message from node $key")
            }
        }
        language = tmp
    }
}
