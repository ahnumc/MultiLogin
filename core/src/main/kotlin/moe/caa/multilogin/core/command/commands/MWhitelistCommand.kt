package moe.caa.multilogin.core.command.commands

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import moe.caa.multilogin.api.internal.plugin.ISender
import moe.caa.multilogin.core.command.CommandHandler
import moe.caa.multilogin.core.command.Permissions
import moe.caa.multilogin.core.command.argument.OnlineArgumentType
import moe.caa.multilogin.core.command.argument.StringArgumentType

/**
 * /MultiLogin whitelist * 指令处理程序
 */
class MWhitelistCommand(private val handler: CommandHandler) {
    fun register(literalArgumentBuilder: LiteralArgumentBuilder<ISender?>): LiteralArgumentBuilder<ISender?>? {
        return literalArgumentBuilder
            .then(
                handler.literal("add")
                    .requires { it?.hasPermission(Permissions.COMMAND_MULTI_LOGIN_WHITELIST_ADD) ?: false }
                    .then(
                        handler.argument("username", StringArgumentType.string())
                            .executes { this.executeAddUsername(it) }
                    )
            )
            .then(
                handler.literal("remove")
                    .requires { it?.hasPermission(Permissions.COMMAND_MULTI_LOGIN_WHITELIST_REMOVE) ?: false }
                    .then(
                        handler.argument("username", StringArgumentType.string())
                            .executes { this.executeRemoveUsername(it) }
                    )
            ).then(
                handler.literal("specific")
                    .then(
                        handler.literal("add")
                            .requires {
                                it?.hasPermission(Permissions.COMMAND_MULTI_LOGIN_WHITELIST_SPECIFIC_ADD) ?: false
                            }
                            .then(
                                handler.argument("online", OnlineArgumentType.online())
                                    .executes { this.executeAdd(it) }
                            )
                    )
                    .then(
                        handler.literal("remove")
                            .requires {
                                it?.hasPermission(Permissions.COMMAND_MULTI_LOGIN_WHITELIST_SPECIFIC_REMOVE) ?: false
                            }
                            .then(
                                handler.argument("online", OnlineArgumentType.online())
                                    .executes { this.executeRemove(it) }
                            )
                    )
            ).then(
                handler.literal("list")
                    .requires { it?.hasPermission(Permissions.COMMAND_MULTI_LOGIN_WHITELIST_LIST) ?: false }
                    .executes { this.executeList(it) }
                    .then(
                        handler.literal("verbose")
                            .requires {
                                it?.hasPermission(Permissions.COMMAND_MULTI_LOGIN_WHITELIST_LIST_VERBOSE) ?: false
                            }
                            .executes { this.executeListVerbose(it) }
                    )
            )
    }

    private fun executeRemove(context: CommandContext<ISender?>): Int {
        val online = OnlineArgumentType.getOnline(context, "online")
        val sender = requireNotNull(context.source)
        val core = CommandHandler.core
        if (online.whitelist != true) {
            sender.sendMessagePL(
                core.languageHandler.getMessage(
                    "command_message_whitelist_permanent_remove_repeat",
                    "online_uuid" to online.onlineUUID,
                    "online_name" to online.onlineName,
                    "service_name" to online.baseServiceConfig.serviceName,
                    "service_id" to online.baseServiceConfig.serviceId
                )
            )
            return 0
        }
        core.sqlManager.userDataTable.setWhitelist(online.onlineUUID, online.baseServiceConfig.serviceId, false)
        sender.sendMessagePL(
            core.languageHandler.getMessage(
                "command_message_whitelist_permanent_remove",
                "online_uuid" to online.onlineUUID,
                "online_name" to online.onlineName,
                "service_name" to online.baseServiceConfig.serviceName,
                "service_id" to online.baseServiceConfig.serviceId
            )
        )
        core.sqlManager.userDataTable.getInGameUUID(online.onlineUUID, online.baseServiceConfig.serviceId)
            ?.let { inGameUUID ->
                core.plugin.runServer.playerManager.kickPlayerIfOnline(
                    inGameUUID,
                    core.languageHandler.getMessage("in_game_whitelist_removed")
                )
            }
        return 0
    }

    private fun executeAdd(context: CommandContext<ISender?>): Int {
        val online = OnlineArgumentType.getOnline(context, "online")
        val sender = requireNotNull(context.source)
        val core = CommandHandler.core
        if (online.whitelist) {
            sender.sendMessagePL(
                core.languageHandler.getMessage(
                    "command_message_whitelist_permanent_add_repeat",
                    "online_uuid" to online.onlineUUID,
                    "online_name" to online.onlineName,
                    "service_name" to online.baseServiceConfig.serviceName,
                    "service_id" to online.baseServiceConfig.serviceId
                )
            )
            return 0
        }
        if (!core.sqlManager.userDataTable.dataExists(online.onlineUUID, online.baseServiceConfig.serviceId)) {
            core.sqlManager.userDataTable.insertNewData(
                online.onlineUUID,
                online.baseServiceConfig.serviceId,
                null,
                null
            )
        }
        core.sqlManager.userDataTable.setWhitelist(online.onlineUUID, online.baseServiceConfig.serviceId, true)
        sender.sendMessagePL(
            core.languageHandler.getMessage(
                "command_message_whitelist_permanent_add",
                "online_uuid" to online.onlineUUID,
                "online_name" to online.onlineName,
                "service_name" to online.baseServiceConfig.serviceName,
                "service_id" to online.baseServiceConfig.serviceId
            )
        )
        return 0
    }

    private fun executeRemoveUsername(context: CommandContext<ISender?>): Int {
        val username = StringArgumentType.getString(context, "username")
        val sender = requireNotNull(context.source)
        val core = CommandHandler.core
        var count = 0
        if (core.cacheWhitelistHandler.cachedWhitelist.remove(username)) count++
        val inGameUUID = core.sqlManager.inGameProfileTable.getInGameUUIDIgnoreCase(username)
        if (inGameUUID != null && core.sqlManager.userDataTable.hasWhitelist(inGameUUID)) {
            count++
            core.sqlManager.userDataTable.setWhitelist(inGameUUID, false)
        }
        if (count == 0) {
            sender.sendMessagePL(
                core.languageHandler.getMessage(
                    "command_message_whitelist_remove_repeat",
                    "name" to username
                )
            )
            return 0
        }
        sender.sendMessagePL(
            core.languageHandler.getMessage(
                "command_message_whitelist_remove",
                "name" to username,
                "count" to count
            )
        )
        inGameUUID?.let {
            core.plugin.runServer.playerManager.getPlayer(it)
                ?.kickPlayer(core.languageHandler.getMessage("in_game_whitelist_removed"))
        }
        return 0
    }

    private fun executeAddUsername(context: CommandContext<ISender?>): Int {
        val username = StringArgumentType.getString(context, "username").lowercase()
        val sender = requireNotNull(context.source)
        val core = CommandHandler.core
        val inGameUUID = core.sqlManager.inGameProfileTable.getInGameUUIDIgnoreCase(username)
        val have = inGameUUID?.let { core.sqlManager.userDataTable.hasWhitelist(it) } ?: false
        if (have || !core.cacheWhitelistHandler.cachedWhitelist.add(username)) {
            sender.sendMessagePL(
                core.languageHandler.getMessage(
                    "command_message_whitelist_add_repeat",
                    "name" to username
                )
            )
            return 0
        }
        sender.sendMessagePL(
            core.languageHandler.getMessage(
                "command_message_whitelist_add",
                "name" to username
            )
        )
        return 0
    }

    private fun executeList(context: CommandContext<ISender?>, verbose: Boolean = false): Int {
        val sender = requireNotNull(context.source)
        val core = CommandHandler.core
        val list = core.sqlManager.userDataTable.listWhitelist(verbose)
        sender.sendMessagePL(
            core.languageHandler.getMessage(
                "command_message_whitelist_list_table",
                "count" to list.size,
                "list" to list.joinToString(if (verbose) ", \n" else ", ")
            )
        )

        val cache = core.cacheWhitelistHandler.cachedWhitelist
        sender.sendMessagePL(
            core.languageHandler.getMessage(
                "command_message_whitelist_list_cache",
                "list" to cache.joinToString(", "),
                "count" to cache.size
            )
        )
        return 0
    }

    private fun executeListVerbose(context: CommandContext<ISender?>): Int = executeList(context, true)
}
