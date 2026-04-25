package `fun`.ksnb.multilogin.velocity.impl

import com.velocitypowered.api.proxy.ProxyServer
import com.velocitypowered.proxy.config.PlayerInfoForwarding
import com.velocitypowered.proxy.config.VelocityConfiguration
import moe.caa.multilogin.api.internal.plugin.BaseScheduler
import moe.caa.multilogin.api.internal.plugin.IPlayerManager
import moe.caa.multilogin.api.internal.plugin.ISender
import moe.caa.multilogin.api.internal.plugin.IServer

class VelocityServer(private val server: ProxyServer) : IServer {
    override val scheduler: BaseScheduler = VelocityScheduler()
    override val playerManager: IPlayerManager = VelocityPlayerManager(server)

    override val isOnlineMode: Boolean
        get() = server.configuration.isOnlineMode

    override val isForwarded: Boolean
        get() = (server.configuration as VelocityConfiguration).playerInfoForwardingMode != PlayerInfoForwarding.NONE

    override val name: String
        get() = server.version.name

    override val version: String
        get() = server.version.version

    override fun shutdown() {
        server.shutdown()
    }

    override val consoleSender: ISender
        get() = VelocitySender(server.consoleCommandSource)

    override fun pluginHasEnabled(id: String): Boolean =
        server.pluginManager.plugins.any { it.description.name.map { n -> n.equals(id, ignoreCase = true) }.orElse(false) }
}
