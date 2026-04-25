package `fun`.ksnb.multilogin.velocity.impl

import com.velocitypowered.api.command.CommandSource
import com.velocitypowered.api.proxy.Player
import com.velocitypowered.proxy.console.VelocityConsole
import moe.caa.multilogin.api.internal.plugin.IPlayer
import moe.caa.multilogin.api.internal.plugin.ISender
import net.kyori.adventure.text.Component

/**
 * Velocity 指令执行者对象
 */
open class VelocitySender(private val commandSource: CommandSource) : ISender {
    override val isPlayer: Boolean
        get() = commandSource is Player

    override val isConsole: Boolean
        get() = commandSource is VelocityConsole

    override fun hasPermission(permission: String): Boolean {
        return commandSource.hasPermission(permission)
    }

    override fun sendMessagePL(message: String) {
        for (s in message.split("\\r?\\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()) {
            commandSource.sendMessage(Component.text(s))
        }
    }

    override val name: String
        get() = "CONSOLE"

    override val asPlayer: IPlayer?
        get() = (commandSource as? Player)?.let(::VelocityPlayer)
}
