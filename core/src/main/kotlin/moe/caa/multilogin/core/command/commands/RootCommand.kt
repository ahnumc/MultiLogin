package moe.caa.multilogin.core.command.commands

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import moe.caa.multilogin.api.internal.plugin.ISender
import moe.caa.multilogin.api.internal.util.Pair
import moe.caa.multilogin.core.command.CommandHandler
import moe.caa.multilogin.core.command.Permissions
import moe.caa.multilogin.core.command.argument.StringArgumentType
import moe.caa.multilogin.core.command.submitConfirm

class RootCommand(private val handler: CommandHandler) {
    fun register(literalArgumentBuilder: LiteralArgumentBuilder<ISender?>): LiteralArgumentBuilder<ISender?>? {
        return literalArgumentBuilder
            .then(
                handler.literal("reload")
                    .requires { it?.hasPermission(Permissions.COMMAND_MULTI_LOGIN_RELOAD) ?: false }
                    .executes { this.executeReload(it) }
            )
            .then(
                handler.literal("eraseUsername")
                    .requires { it?.hasPermission(Permissions.COMMAND_MULTI_LOGIN_ERASE_USERNAME) ?: false }
                    .then(
                        handler.argument("username", StringArgumentType.string())
                            .executes { this.executeEraseUsername(it) }
                    )
            )
            .then(
                handler.literal("eraseAllUsernames")
                    .requires { it?.hasPermission(Permissions.COMMAND_MULTI_LOGIN_ERASE_ALL_USERNAMES) ?: false }
                    .executes { this.executeEraseAllUsernames(it) }
            )
            .then(
                handler.literal("confirm")
                    .requires { it?.hasPermission(Permissions.COMMAND_MULTI_LOGIN_CONFIRM) ?: false }
                    .executes { this.executeConfirm(it) }
            )
            .then(
                handler.literal("list")
                    .requires { it?.hasPermission(Permissions.COMMAND_MULTI_LOGIN_LIST) ?: false }
                    .executes { this.executeList(it) }
            )
            .then(MWhitelistCommand(handler).register(handler.literal("whitelist")))
            .then(MProfileCommand(handler).register(handler.literal("profile")))
            .then(MRenameCommand(handler).register(handler.literal("rename")))
            .then(MFindCommand(handler).register(handler.literal("find")))
            .then(MInfoCommand(handler).register(handler.literal("info")))
            .then(MLinkCommand(handler).register(handler.literal("link")))
            .then(MDataConvert(handler).register(handler.literal("dataconvert")))
    }

    private fun executeList(context: CommandContext<ISender?>): Int {
        val sender = requireNotNull(context.source)
        val core = requireNotNull(CommandHandler.core)
        val onlinePlayers = core.plugin.runServer.playerManager.onlinePlayers

        val identifiedPlayerMap = mutableMapOf<Int, MutableList<moe.caa.multilogin.api.internal.plugin.IPlayer>>()
        for (player in onlinePlayers) {
            val sid = core.playerHandler.getPlayerOnlineProfile(player.uniqueId)?.value2 ?: -1
            identifiedPlayerMap.getOrPut(sid) { mutableListOf() }.add(player)
        }
        core.pluginConfig.serviceIdMap.keys.filterNotNull().forEach { key ->
            identifiedPlayerMap.getOrPut(key) { mutableListOf() }
        }

        val delimiter = core.languageHandler.getMessage("command_message_list_delimiter")
        val playerDelimiter = core.languageHandler.getMessage("command_message_list_player_delimiter")

        val listStr = identifiedPlayerMap.entries.joinToString(delimiter ?: "") { (key, players) ->
            val sname = when {
                key == -1 -> core.languageHandler.getMessage("command_message_list_unidentified_entry_name")
                else -> core.pluginConfig.serviceIdMap[key]?.serviceName
                    ?: core.languageHandler.getMessage("command_message_list_unknown_entry_name")
            }
            val playerListStr = players.joinToString(playerDelimiter ?: "") { p ->
                core.languageHandler.getMessage(
                    "command_message_list_player_entry",
                    Pair<Any?, Any?>("name", p.name)
                )
            }
            core.languageHandler.getMessage(
                "command_message_list_entry",
                Pair<Any?, Any?>("service_name", sname),
                Pair<Any?, Any?>("service_id", key),
                Pair<Any?, Any?>("count", players.size),
                Pair<Any?, Any?>("list", playerListStr)
            )
        }

        sender.sendMessagePL(
            core.languageHandler.getMessage(
                "command_message_list",
                Pair<Any?, Any?>("list", listStr),
                Pair<Any?, Any?>("count", onlinePlayers.size)
            )
        )
        return 0
    }

    private fun executeEraseAllUsernames(context: CommandContext<ISender?>): Int {
        val sender = requireNotNull(context.source)
        handler.submitConfirm(
            sender,
            "command_message_erase_all_username_desc",
            "command_message_erase_all_username_cq"
        ) {
            val core = requireNotNull(CommandHandler.core)
            val i = core.sqlManager.inGameProfileTable!!.eraseAllUsername()
            val kickMsg = core.languageHandler.getMessage("in_game_username_occupy_all")
            core.plugin.runServer.playerManager.kickAll(kickMsg)
            sender.sendMessagePL(
                core.languageHandler.getMessage(
                    "command_message_erase_all_username_done",
                    Pair<Any?, Any?>("count", i)
                )
            )
        }
        return 0
    }

    @Throws(Exception::class)
    private fun executeConfirm(context: CommandContext<ISender?>): Int {
        handler.secondaryConfirmationHandler.confirm(requireNotNull(context.source))
        return 0
    }

    @Throws(Exception::class)
    private fun executeEraseUsername(context: CommandContext<ISender?>): Int {
        val sender = requireNotNull(context.source)
        val core = requireNotNull(CommandHandler.core)
        val string = StringArgumentType.getString(context, "username").lowercase()
        val ignoreCase = core.sqlManager.inGameProfileTable!!.getInGameUUIDIgnoreCase(string)
            ?: run {
                sender.sendMessagePL(
                    core.languageHandler.getMessage(
                        "command_message_erase_username_none",
                        Pair<Any?, Any?>("name", string)
                    )
                )
                return 0
            }

        handler.submitConfirm(
            sender,
            "command_message_erase_username_desc",
            "command_message_erase_username_cq",
            Pair<Any?, Any?>("name", string)
        ) {
            val i = core.sqlManager.inGameProfileTable!!.eraseUsername(string)
            val kickMsg = core.languageHandler.getMessage(
                "in_game_username_occupy",
                Pair<Any?, Any?>("name", string)
            )
            core.plugin.runServer.playerManager.kickPlayerIfOnline(string, kickMsg)
            val msgKey = if (i == 0) "command_message_erase_username_none" else "command_message_erase_username_done"
            sender.sendMessagePL(
                core.languageHandler.getMessage(msgKey, Pair<Any?, Any?>("name", string))
            )
        }
        return 0
    }

    @Throws(Exception::class)
    private fun executeReload(context: CommandContext<ISender?>): Int {
        val sender = requireNotNull(context.source)
        val core = requireNotNull(CommandHandler.core)
        core.reload()
        sender.sendMessagePL(core.languageHandler.getMessage("command_message_reloaded"))
        return 0
    }
}
