package `fun`.ksnb.multilogin.velocity.impl

import com.velocitypowered.api.proxy.Player
import `fun`.ksnb.multilogin.velocity.main.MultiLoginVelocity
import moe.caa.multilogin.api.internal.plugin.IPlayer
import net.kyori.adventure.text.Component
import java.net.SocketAddress
import java.util.*

/**
 * Velocity 玩家对象
 */
class VelocityPlayer(private val player: Player) : VelocitySender(player), IPlayer {
    override fun kickPlayer(message: String?) {
        player.disconnect(Component.text(message ?: ""))
    }

    override val uniqueId: UUID
        get() = player.uniqueId

    override val address: SocketAddress
        get() = player.remoteAddress

    override val isOnline: Boolean
        get() = MultiLoginVelocity.getInstance().runServer.playerManager.getPlayer(player.uniqueId) != null

    override val name: String
        get() = player.username

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is VelocityPlayer) return false
        val that = other
        return player.uniqueId == that.player.uniqueId
    }

    override fun hashCode(): Int = player.uniqueId.hashCode()
}
