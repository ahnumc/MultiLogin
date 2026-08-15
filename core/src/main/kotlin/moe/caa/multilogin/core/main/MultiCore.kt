package moe.caa.multilogin.core.main

import kotlinx.serialization.json.Json
import moe.caa.multilogin.api.MapperConfigAPI
import moe.caa.multilogin.api.MultiLoginAPI
import moe.caa.multilogin.api.MultiLoginAPIProvider.setApi
import moe.caa.multilogin.api.data.MultiLoginPlayerData
import moe.caa.multilogin.api.internal.logger.LoggerProvider
import moe.caa.multilogin.api.internal.main.MultiCoreAPI
import moe.caa.multilogin.api.internal.plugin.IPlugin
import moe.caa.multilogin.core.auth.AuthHandler
import moe.caa.multilogin.core.auth.service.floodgate.FloodgateAuthenticationService
import moe.caa.multilogin.core.command.CommandHandler
import moe.caa.multilogin.core.configuration.PluginConfig
import moe.caa.multilogin.core.configuration.service.BaseServiceConfig
import moe.caa.multilogin.core.database.SQLManager
import moe.caa.multilogin.core.handle.CacheWhitelistHandler
import moe.caa.multilogin.core.handle.PlayerHandler
import moe.caa.multilogin.core.language.LanguageHandler
import moe.caa.multilogin.core.semver.CheckUpdater
import moe.caa.multilogin.core.semver.SemVersion
import moe.caa.multilogin.core.skinrestorer.SkinRestorerCore
import java.io.IOException
import java.net.URISyntaxException
import java.sql.SQLException
import java.util.*

/**
 * 猫踢核心
 */
class MultiCore(override val plugin: IPlugin) : MultiCoreAPI, MultiLoginAPI {
    val buildManifest: BuildManifest = BuildManifest(this)
    val sqlManager: SQLManager = SQLManager(this)
    val pluginConfig: PluginConfig = PluginConfig(plugin.dataFolder, this)
    override val authHandler: AuthHandler = AuthHandler(this)
    override val skinRestorerHandler: SkinRestorerCore = SkinRestorerCore(this)
    override val commandHandler: CommandHandler = CommandHandler(this)
    override val languageHandler: LanguageHandler = LanguageHandler(this)
    override val playerHandler: PlayerHandler = PlayerHandler(this)
    val cacheWhitelistHandler: CacheWhitelistHandler = CacheWhitelistHandler()
    val gson: Json = Json { ignoreUnknownKeys = true }
    var semVersion: SemVersion? = null
    var floodgateSupported = false
    val httpRequestHeaderUserAgent = "MultiLogin/v2.0"

    private fun setupFloodgate() {
        if (plugin.runServer.pluginHasEnabled("floodgate")) {
            try {
                FloodgateAuthenticationService(this).register()
                LoggerProvider.logger.info("Floodgate detected, service registered.")
                floodgateSupported = true
            } catch (e: Throwable) {
                floodgateSupported = false
                LoggerProvider.logger.error("Unable to load floodgate handler, is it up to date?", e)
            }
        }
    }

    private fun showBanner() {
        plugin.runServer.consoleSender.sendMessagePL("\u001b[40;31m __  __       _ _   _ _                _       \u001b[0m")
        plugin.runServer.consoleSender.sendMessagePL("\u001b[40;33m|  \\/  |_   _| | |_(_) |    ___   __ _(_)_ __  \u001b[0m")
        plugin.runServer.consoleSender.sendMessagePL("\u001b[40;32m| |\\/| | | | | | __| | |   / _ \\ / _` | | '_ \\ \u001b[0m")
        plugin.runServer.consoleSender.sendMessagePL("\u001b[40;36m| |  | | |_| | | |_| | |__| (_) | (_| | | | | |\u001b[0m")
        plugin.runServer.consoleSender.sendMessagePL("\u001b[40;34m|_|  |_|\\__,_|_|\\__|_|_____\\___/ \\__, |_|_| |_|\u001b[0m")
        plugin.runServer.consoleSender.sendMessagePL("\u001b[40;35m                                 |___/         \u001b[0m")
    }

    @Throws(IOException::class, SQLException::class, ClassNotFoundException::class, URISyntaxException::class)
    override fun load() {
        setApi(this)
        if (!plugin.dataFolder.exists() && !plugin.dataFolder.mkdirs()) {
            throw IOException("Unable to create plugin data folder: ${plugin.dataFolder}")
        }

        showBanner()
        buildManifest.read()
        buildManifest.checkStable()

        setupFloodgate()
        languageHandler.init()
        pluginConfig.reload()
        sqlManager.init()
        commandHandler.init()
        playerHandler.register()
        CheckUpdater(this).start()

        this.semVersion = SemVersion.of(buildManifest.version)
        LoggerProvider.logger.info(
            "Loaded, using MultiLogin v%s on %s - %s".format(
                buildManifest.version, plugin.runServer.name, plugin.runServer.version
            )
        )
        checkEnvironment()

        try {
            MetricsLite(this)
        } catch (throwable: Throwable) {
            LoggerProvider.logger.error(throwable)
        }
    }

    private fun checkEnvironment() {
        if (!plugin.runServer.isOnlineMode) {
            LoggerProvider.logger.error("Please enable online mode, otherwise the plugin will not work!!!")
            LoggerProvider.logger.error("Server is closing!!!")
            throw EnvironmentException("offline mode.")
        }
        if (!plugin.runServer.isForwarded) {
            LoggerProvider.logger.error("Please enable forwarding, otherwise the plugin will not work!!!")
            LoggerProvider.logger.error("Server is closing!!!")
            throw EnvironmentException("do not forward.")
        }
    }

    @Throws(IOException::class, URISyntaxException::class)
    fun reload() {
        pluginConfig.reload()
        languageHandler.reload()
    }

    override fun close() {
        sqlManager.close()
    }

    override val mapperConfig: MapperConfigAPI
        get() = requireNotNull(pluginConfig.mapperConfig)

    override val services: MutableCollection<BaseServiceConfig>
        get() = pluginConfig.serviceIdMap.values.toMutableList()

    override fun getPlayerData(inGameUUID: UUID): MultiLoginPlayerData? {
        return playerHandler.getPlayerData(inGameUUID)
    }
}
