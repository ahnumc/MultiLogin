package `fun`.ksnb.multilogin.velocity.main

import com.google.inject.Inject
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent
import com.velocitypowered.api.plugin.annotation.DataDirectory
import com.velocitypowered.proxy.VelocityServer
import `fun`.ksnb.multilogin.velocity.logger.Slf4jLoggerBridge
import moe.caa.multilogin.api.internal.injector.Injector
import moe.caa.multilogin.api.internal.logger.LoggerProvider
import moe.caa.multilogin.api.internal.main.MultiCoreAPI
import moe.caa.multilogin.api.internal.plugin.IPlugin
import moe.caa.multilogin.core.main.MultiCore
import moe.caa.multilogin.velocity.injector.VelocityInjector
import org.slf4j.Logger
import java.io.File
import java.nio.file.Path

/**
 * Velocity Main
 */
class MultiLoginVelocity @Inject constructor(
    server: com.velocitypowered.api.proxy.ProxyServer,
    logger: Logger,
    @param:DataDirectory private val dataDirectory: Path
) : IPlugin {
    val server: VelocityServer
    override val runServer: `fun`.ksnb.multilogin.velocity.impl.VelocityServer
    var multiCoreAPI: MultiCoreAPI? = null
    private lateinit var injector: Injector
    private val coreApi: MultiCoreAPI
        get() = requireNotNull(multiCoreAPI)

    init {
        instance = this
        this.server = server as VelocityServer
        this.runServer = `fun`.ksnb.multilogin.velocity.impl.VelocityServer(this.server)
        LoggerProvider.logger = Slf4jLoggerBridge(logger)
    }

    @Subscribe
    fun onInitialize(event: ProxyInitializeEvent) {
        try {
            multiCoreAPI = MultiCore(this)
            coreApi.load()
            injector = VelocityInjector()
            injector.inject(coreApi)
        } catch (e: Throwable) {
            LoggerProvider.logger.error("An exception was encountered while loading the plugin.", e)
            server.shutdown()
            return
        }
        GlobalListener(this).register()
        CommandHandler(this).register("multilogin")

    }

    @Subscribe
    fun onDisable(event: ProxyShutdownEvent) {
        try {
            multiCoreAPI?.close()
        } catch (e: Exception) {
            LoggerProvider.logger.error("An exception was encountered while close the plugin", e)
        } finally {
            multiCoreAPI = null
            server.shutdown()
        }
    }

    override val dataFolder: File
        get() = dataDirectory.toFile()

    override val tempFolder: File
        get() = File(this.dataFolder, "tmp")

    companion object {
        private lateinit var instance: MultiLoginVelocity
        @JvmStatic
        fun getInstance(): MultiLoginVelocity = instance
    }
}
