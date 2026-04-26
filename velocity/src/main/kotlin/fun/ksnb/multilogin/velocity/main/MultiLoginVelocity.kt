package `fun`.ksnb.multilogin.velocity.main

import com.google.inject.Inject
import com.velocitypowered.api.event.AwaitingEventExecutor
import com.velocitypowered.api.event.EventTask
import com.velocitypowered.api.event.PostOrder
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.connection.DisconnectEvent
import com.velocitypowered.api.event.connection.PostLoginEvent
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent
import com.velocitypowered.api.plugin.annotation.DataDirectory
import com.velocitypowered.api.proxy.Player
import com.velocitypowered.proxy.VelocityServer
import com.velocitypowered.proxy.connection.client.ConnectedPlayer
import com.velocitypowered.proxy.network.Connections
import `fun`.ksnb.multilogin.velocity.impl.ChatSessionHandler
import `fun`.ksnb.multilogin.velocity.impl.NewChatSessionPacketIDEvent
import `fun`.ksnb.multilogin.velocity.logger.Slf4jLoggerBridge
import moe.caa.multilogin.api.internal.injector.Injector
import moe.caa.multilogin.api.internal.logger.LoggerProvider
import moe.caa.multilogin.api.internal.main.MultiCoreAPI
import moe.caa.multilogin.api.internal.plugin.IPlugin
import moe.caa.multilogin.loader.main.PluginLoader
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
    private val pluginLoader: PluginLoader
    var multiCoreAPI: MultiCoreAPI? = null
    private lateinit var injector: Injector
    private val coreApi: MultiCoreAPI
        get() = requireNotNull(multiCoreAPI)

    init {
        instance = this
        this.server = server as VelocityServer
        this.runServer = `fun`.ksnb.multilogin.velocity.impl.VelocityServer(this.server)
        LoggerProvider.logger = Slf4jLoggerBridge(logger)
        this.pluginLoader = PluginLoader(this)
        try {
            pluginLoader.load("MultiLogin-Velocity-Injector.JarFile")
        } catch (e: Exception) {
            LoggerProvider.logger.error("An exception was encountered while initializing the plugin.", e)
            server.shutdown()
        }
    }

    @Subscribe
    fun onInitialize(event: ProxyInitializeEvent) {
        try {
            multiCoreAPI = pluginLoader.coreObject
            coreApi.load()
            injector = pluginLoader.findClass("moe.caa.multilogin.velocity.injector.VelocityInjector").getConstructor()
                .newInstance() as Injector
            injector.inject(coreApi)
            injector.registerChatSession(coreApi.mapperConfig.packetMapping)
        } catch (e: Throwable) {
            LoggerProvider.logger.error("An exception was encountered while loading the plugin.", e)
            server.shutdown()
            return
        }
        GlobalListener(this).register()
        CommandHandler(this).register("multilogin")

        server.eventManager.register(
            this, PostLoginEvent::class.java,
            AwaitingEventExecutor { postLoginEvent: PostLoginEvent ->
                EventTask.withContinuation { continuation ->
                    try {
                        if (postLoginEvent.player.protocolVersion.protocol < 761) return@withContinuation
                        injectPlayer(postLoginEvent.player)
                    } finally {
                        continuation.resume()
                    }
                }
            }
        )
        server.eventManager.register(
            this, DisconnectEvent::class.java, PostOrder.LAST,
            AwaitingEventExecutor { disconnectEvent: DisconnectEvent ->
                if (disconnectEvent.loginStatus == DisconnectEvent.LoginStatus.CONFLICTING_LOGIN)
                    null
                else
                    EventTask.async { removePlayer(disconnectEvent.player) }
            }
        )
        server.eventManager.register(
            this, NewChatSessionPacketIDEvent::class.java,
            AwaitingEventExecutor { packetEvent: NewChatSessionPacketIDEvent ->
                EventTask.withContinuation { continuation ->
                    runServer.playerManager.kickPlayerIfOnline(
                        packetEvent.player.uniqueId,
                        coreApi.languageHandler.getMessage("reconnect_msg")
                    )
                    coreApi.mapperConfig.packetMapping[packetEvent.version.protocol] = packetEvent.packetID
                    coreApi.mapperConfig.save()
                    injector.registerChatSession(coreApi.mapperConfig.packetMapping)
                    continuation.resume()
                }
            }
        )
    }

    @Subscribe
    fun onDisable(event: ProxyShutdownEvent) {
        try {
            multiCoreAPI?.close()
            pluginLoader.close()
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

    private fun injectPlayer(player: Player) {
        val connectedPlayer = player as ConnectedPlayer
        connectedPlayer.connection
            .channel
            .pipeline()
            .addBefore(Connections.HANDLER, KEY, ChatSessionHandler(player, server.eventManager))
    }

    private fun removePlayer(player: Player) {
        val connectedPlayer = player as ConnectedPlayer
        val channel = connectedPlayer.connection.channel
        channel.eventLoop().submit {
            channel.pipeline().remove(KEY)
        }
    }

    companion object {
        private lateinit var instance: MultiLoginVelocity
        private const val KEY = "MultiLoginChatSession"

        @JvmStatic
        fun getInstance(): MultiLoginVelocity = instance
    }
}
