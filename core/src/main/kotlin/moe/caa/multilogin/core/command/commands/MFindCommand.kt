package moe.caa.multilogin.core.command.commands

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import moe.caa.multilogin.api.internal.plugin.ISender
import moe.caa.multilogin.core.command.CommandHandler
import moe.caa.multilogin.core.command.Permissions
import moe.caa.multilogin.core.command.argument.OnlineArgumentType
import moe.caa.multilogin.core.command.argument.ProfileArgumentType

class MFindCommand(private val handler: CommandHandler) {
    fun register(literalArgumentBuilder: LiteralArgumentBuilder<ISender?>): LiteralArgumentBuilder<ISender?>? {
        return literalArgumentBuilder
            .then(
                handler.literal("profile")
                    .requires { it?.hasPermission(Permissions.COMMAND_MULTI_LOGIN_FIND_PROFILE) ?: false }
                    .then(
                        handler.argument("profile", ProfileArgumentType.profile())
                            .executes { this.executeProfile(it) }
                    )
            )
            .then(
                handler.literal("online")
                    .requires { it?.hasPermission(Permissions.COMMAND_MULTI_LOGIN_FIND_ONLINE) ?: false }
                    .then(
                        handler.argument("online", OnlineArgumentType.online())
                            .executes { this.executeOnline(it) }
                    )
            )
    }

    private fun executeOnline(context: CommandContext<ISender?>): Int {
        val online = OnlineArgumentType.getOnline(context, "online")
        val sender = requireNotNull(context.source)
        val core = CommandHandler.core
        val whitelist = if (online.whitelist == true)
            core.languageHandler.getMessage("command_message_find_online_whitelist_true")
        else
            core.languageHandler.getMessage("command_message_find_online_whitelist_false")

        val profileUUID = online.profileUUID
        val profileInfo = if (profileUUID == null) {
            core.languageHandler.getMessage("command_message_find_online_profilenotexist")
        } else {
            val profileName = core.sqlManager.inGameProfileTable.getUsername(profileUUID)
                ?: core.languageHandler.getMessage("command_message_find_online_profileunnamed")
            core.languageHandler.getMessage(
                "command_message_find_online_profile",
                "profile_uuid" to profileUUID,
                "profile_name" to profileName
            )
        }

        sender.sendMessagePL(
            core.languageHandler.getMessage(
                "command_message_find_online",
                "service_name" to online.baseServiceConfig.serviceName,
                "service_id" to online.baseServiceConfig.serviceId,
                "online_uuid" to online.onlineUUID,
                "online_name" to online.onlineName,
                "whitelist" to whitelist,
                "profile" to profileInfo
            )
        )
        return 0
    }

    private fun executeProfile(context: CommandContext<ISender?>): Int {
        val profile = ProfileArgumentType.getProfile(context, "profile")
        val profileUUID = profile.profileUUID
        val sender = requireNotNull(context.source)
        val core = CommandHandler.core
        val onlineProfiles = core.sqlManager.userDataTable.getOnlineProfiles(profileUUID)
        val profileName = core.sqlManager.inGameProfileTable.getUsername(profileUUID)
            ?: core.languageHandler.getMessage("command_message_find_profile_entry_unnamed")

        val delimiter = core.languageHandler.getMessage("command_message_find_profile_entry_delimiter")
        val listStr = onlineProfiles.joinToString(delimiter ?: "") { raw ->
            val p = requireNotNull(raw)
            val serviceName = core.pluginConfig.serviceIdMap[p.third]?.serviceName
                ?: core.languageHandler.getMessage("command_message_find_profile_entry_unused_service")
            core.languageHandler.getMessage(
                "command_message_find_profile_entry",
                "service_name" to serviceName,
                "service_id" to p.third,
                "online_uuid" to p.first,
                "online_name" to (p.second
                    ?: core.languageHandler.getMessage("command_message_find_profile_entry_onlineunnamed"))
            )
        }

        sender.sendMessagePL(
            core.languageHandler.getMessage(
                "command_message_find_profile",
                "profile_uuid" to profileUUID,
                "profile_name" to profileName,
                "count" to onlineProfiles.size,
                "list" to listStr
            )
        )
        return 0
    }
}
