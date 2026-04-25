package `fun`.ksnb.multilogin.velocity.impl

import com.velocitypowered.api.proxy.ProxyServer
import moe.caa.multilogin.api.internal.plugin.IPlayer
import moe.caa.multilogin.api.internal.plugin.IPlayerManager
import java.util.*

class VelocityPlayerManager(private val server: ProxyServer) : IPlayerManager {
    override fun getPlayers(name: String): MutableSet<IPlayer> =
        server.allPlayers
            .filter { it.username.equals(name, ignoreCase = true) }
            .map { VelocityPlayer(it) }
            .toMutableSet()

    override fun getPlayer(uuid: UUID): IPlayer? =
        server.getPlayer(uuid).orElse(null)?.let { VelocityPlayer(it) }

    override val onlinePlayers: MutableSet<IPlayer>
        get() = server.allPlayers.map { VelocityPlayer(it) }.toMutableSet()
}
