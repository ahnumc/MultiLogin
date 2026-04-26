package moe.caa.multilogin.core.command.commands

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.CommandSyntaxException
import moe.caa.multilogin.api.internal.plugin.IPlayer
import moe.caa.multilogin.api.internal.plugin.ISender
import moe.caa.multilogin.core.command.CommandHandler
import moe.caa.multilogin.core.command.Permissions
import moe.caa.multilogin.core.command.argument.OnlinePlayerArgumentType

class MInfoCommand(private val handler: CommandHandler) {
    fun register(literalArgumentBuilder: LiteralArgumentBuilder<ISender?>): LiteralArgumentBuilder<ISender?>? {
        return literalArgumentBuilder.then(
            handler.argument<MutableSet<IPlayer>>("player", OnlinePlayerArgumentType.players())
                .requires { it?.hasPermission(Permissions.COMMAND_MULTI_LOGIN_CURRENT_OTHER) ?: false }
                .executes { this.executeInfo(it) }
        )
            .requires { it?.hasPermission(Permissions.COMMAND_MULTI_LOGIN_CURRENT_ONESELF) ?: false }
            .executes { this.executeInfoOneself(it) }
    }

    private fun executeInfo(context: CommandContext<ISender?>): Int {
        val players = OnlinePlayerArgumentType.getPlayers(context, "player")
        processInfoCommand(context, players)
        return 0
    }

    @Throws(CommandSyntaxException::class)
    private fun executeInfoOneself(context: CommandContext<ISender?>): Int {
        handler.requirePlayer(context)
        processInfoCommand(context, mutableSetOf(requireNotNull(context.source?.asPlayer)))
        return 0
    }

    private fun processInfoCommand(context: CommandContext<ISender?>, players: MutableSet<IPlayer>) {
        val sender = requireNotNull(context.source)
        val core = CommandHandler.core
        if (players.size > 1) {
            sender.sendMessagePL(
                core.languageHandler.getMessage(
                    "command_message_info_multi", "size" to players.size
                )
            )
        }

        for (player in players) {
            val profile = core.playerHandler.getPlayerOnlineProfile(player.uniqueId)
            if (profile == null) {
                sender.sendMessagePL(
                    core.languageHandler.getMessage(
                        "command_message_info_unknown",
                        "name" to player.name,
                        "uuid" to player.uniqueId
                    )
                )
            } else {
                val onlineProfile = profile.first
                val serviceId = profile.second
                val serviceName = core.pluginConfig.serviceIdMap[serviceId]?.serviceName
                    ?: core.languageHandler.getMessage("command_message_info_unidentified_name")
                val message = core.languageHandler.getMessage(
                    "command_message_info",
                    "name" to player.name,
                    "uuid" to player.uniqueId,
                    "service_name" to serviceName,
                    "service_id" to serviceId,
                    "online_name" to onlineProfile?.name,
                    "online_uuid" to onlineProfile?.id
                )
                sender.sendMessagePL(message)
            }
        }
    }
}
