package moe.caa.multilogin.core.language

import moe.caa.multilogin.api.internal.language.LanguageAPI
import moe.caa.multilogin.api.internal.logger.LoggerProvider
import moe.caa.multilogin.api.internal.util.IOUtil.copy
import moe.caa.multilogin.api.internal.util.Pair
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

    override fun getMessage(node: String?, vararg pairs: Pair<*, *>): String {
        return transPapi(language.getProperty(node), *pairs)
    }

    @Throws(IOException::class)
    fun reload() {
        val tmp = Properties()
        val messagePropertiesFile = File(core.plugin.dataFolder, "message.properties")
        if (!messagePropertiesFile.exists()) {
            FileOutputStream(messagePropertiesFile).use { outputStream ->
                (javaClass.getResourceAsStream("/message.properties")
                    ?: error("Resource not found: /message.properties")).use { copy(it, outputStream) }
            }
            LoggerProvider.logger.info("Extract: message.properties")
        }

        FileInputStream(messagePropertiesFile).use { inputStream ->
            tmp.load(InputStreamReader(inputStream, StandardCharsets.UTF_8))
        }
        (javaClass.getResourceAsStream("/message.properties")
            ?: error("Resource not found: /message.properties")).use { resourceAsStream ->
            InputStreamReader(resourceAsStream, StandardCharsets.UTF_8).use { isr ->
                val inside = Properties()
                inside.load(isr)
                for (entry in inside.entries) {
                    if (entry.key in tmp) continue
                    tmp.setProperty(entry.key.toString(), entry.value.toString())
                    LoggerProvider.logger.warn("Missing message from node ${entry.key}")
                }
            }
        }
        language = tmp
    }
}
