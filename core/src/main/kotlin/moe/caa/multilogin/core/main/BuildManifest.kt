package moe.caa.multilogin.core.main

import moe.caa.multilogin.api.internal.logger.LoggerProvider
import java.io.IOException
import java.util.*

/**
 * 非稳定版本输出Banner
 */
class BuildManifest(private val core: MultiCore?) {
    var buildType: String = ""
        private set
    var buildDate: Date = Date()
        private set
    var version: String = ""
        private set

    @Throws(IOException::class)
    fun read() {
        val properties = Properties()
        properties.load(javaClass.getResourceAsStream("/build.properties"))

        buildType = properties.getProperty("build_type")
        buildDate = Date(properties.getProperty("build_timestamp").toLong())
        version = properties.getProperty("version")
    }

    fun checkStable() {
        if (!buildType.equals("final", ignoreCase = true)) {
            LoggerProvider.logger
                .warn("Please exercise caution and care when using the current version of the multilogin, as it may be unstable.")
        }
    }
}
