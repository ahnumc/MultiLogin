package moe.caa.multilogin.core.command.commands

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.CommandSyntaxException
import moe.caa.multilogin.api.internal.plugin.ISender
import moe.caa.multilogin.api.internal.util.ValueUtil.generateLinkCode
import moe.caa.multilogin.api.profile.GameProfile
import moe.caa.multilogin.core.command.CommandHandler
import moe.caa.multilogin.core.command.Permissions
import moe.caa.multilogin.core.command.argument.OnlinePlayerArgumentType
import moe.caa.multilogin.core.command.argument.StringArgumentType
import moe.caa.multilogin.core.command.submitConfirm
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class MLinkCommand(private val handler: CommandHandler) {
    private val gameProfileEntryMap: MutableMap<GameProfile, Entry> = ConcurrentHashMap()

    fun register(literal: LiteralArgumentBuilder<ISender?>): LiteralArgumentBuilder<ISender?>? {
        return literal
            .then(
                handler.literal("to")
                    .requires { it?.hasPermission(Permissions.COMMAND_MULTI_LOGIN_LINK_TO) ?: false }
                    .then(
                        handler.argument("player", OnlinePlayerArgumentType.players())
                            .executes { this.executeLinkTo(it) }
                    )
            )
            .then(
                handler.literal("accept")
                    .requires { it?.hasPermission(Permissions.COMMAND_MULTI_LOGIN_LINK_ACCEPT) ?: false }
                    .then(
                        handler.argument("name", StringArgumentType.string())
                            .executes { this.executeLinkAccept(it) }
                    )
            )
            .then(
                handler.literal("code")
                    .requires { it?.hasPermission(Permissions.COMMAND_MULTI_LOGIN_LINK_CODE) ?: false }
                    .then(
                        handler.argument("player", OnlinePlayerArgumentType.players())
                            .then(
                                handler.argument("code", StringArgumentType.string())
                                    .executes { this.executeLinkCode(it) }
                            )
                    )
            )
    }

    @Throws(CommandSyntaxException::class)
    private fun executeLinkCode(context: CommandContext<ISender?>): Int {
        val self = requireNotNull(handler.requireDataCacheArgumentSelf(context).first)
        val target = OnlinePlayerArgumentType.getPlayer(context, "player")
        val code = StringArgumentType.getString(context, "code")
        val sender = requireNotNull(context.source)
        val sourcePlayer = requireNotNull(sender.asPlayer)
        val core = CommandHandler.core

        gameProfileEntryMap.values.removeIf { it.timeMills < System.currentTimeMillis() - 30000 }
        val entry = gameProfileEntryMap[self]
        if (entry == null || entry.receiverUserInGameUUID != target.uniqueId || entry.code == null) {
            sender.sendMessagePL(core.languageHandler.getMessage("command_message_code_invalid"))
            return 0
        }
        if (entry.code != code) {
            sender.sendMessagePL(core.languageHandler.getMessage("command_message_code_invalid_code"))
            return 0
        }
        gameProfileEntryMap.remove(self)

        core.sqlManager.userDataTable.setInGameUUID(
            requireNotNull(entry.requesterOnlineProfile.first?.id),
            requireNotNull(entry.requesterOnlineProfile.second),
            entry.receiverUserInGameUUID
        )
        sender.sendMessagePL(
            core.languageHandler.getMessage(
                "command_message_code_succeed",
                "redirect_name" to target.name,
                "redirect_uuid" to target.uniqueId
            )
        )

        sourcePlayer.kickPlayer(
            core.languageHandler.getMessage(
                "command_message_code_kickmessage",
                "redirect_name" to target.name,
                "redirect_uuid" to target.uniqueId
            )
        )
        return 0
    }

    @Throws(CommandSyntaxException::class)
    private fun executeLinkAccept(context: CommandContext<ISender?>): Int {
        handler.requireDataCacheArgumentSelf(context)
        val string = StringArgumentType.getString(context, "name")
        val sender = requireNotNull(context.source)
        val sourcePlayer = requireNotNull(sender.asPlayer)
        val core = CommandHandler.core
        gameProfileEntryMap.values.removeIf {
            it.timeMills < System.currentTimeMillis() - core.pluginConfig.linkAcceptValidTimeMills
        }
        val entry = gameProfileEntryMap.entries
            .firstOrNull {
                it.key.name.equals(string, ignoreCase = true) &&
                        it.value.receiverUserInGameUUID == sourcePlayer.uniqueId &&
                        it.value.code == null
            }

        if (entry == null) {
            sender.sendMessagePL(core.languageHandler.getMessage("command_message_accept_invalid"))
            return 0
        }

        val targetServiceName = core.pluginConfig.serviceIdMap[entry.value.requesterOnlineProfile.second]?.serviceName
            ?: core.languageHandler.getMessage("command_message_info_unidentified_name")

        handler.submitConfirm(
            sender,
            "command_message_accept_desc",
            "command_message_accept_cq",
            "target_service_name" to targetServiceName,
            "target_service_id" to entry.value.requesterOnlineProfile.second,
            "target_online_name" to entry.key.name,
            "target_online_uuid" to entry.key.id,
            "profile_name" to sourcePlayer.name,
            "profile_uuid" to sourcePlayer.uniqueId
        ) {
            entry.value.code = generateLinkCode()
            sender.sendMessagePL(
                core.languageHandler.getMessage(
                    "command_message_accept",
                    "code" to entry.value.code,
                    "profile_name" to sourcePlayer.name
                )
            )
        }

        return 0
    }

    @Throws(CommandSyntaxException::class)
    private fun executeLinkTo(context: CommandContext<ISender?>): Int {
        val self = handler.requireDataCacheArgumentSelf(context)
        val target = OnlinePlayerArgumentType.getPlayer(context, "player")
        val sender = requireNotNull(context.source)

        handler.requirePlayerAndNoSelf(context, target)
        handler.requireDataCacheArgumentOther(target)

        handler.submitConfirm(
            sender,
            "command_message_link_to_desc",
            "command_message_link_to_cq",
            "redirect_name" to target.name,
            "redirect_uuid" to target.uniqueId
        ) {
            gameProfileEntryMap[requireNotNull(self.first)] = Entry(self, target.uniqueId)
            sender.sendMessagePL(
                CommandHandler.core.languageHandler.getMessage(
                    "command_message_link",
                    "self_online_name" to self.first?.name
                )
            )
        }
        return 0
    }

    class Entry(
        val requesterOnlineProfile: Pair<GameProfile?, Int?>,
        val receiverUserInGameUUID: UUID
    ) {
        val timeMills = System.currentTimeMillis()
        var code: String? = null
    }
}
