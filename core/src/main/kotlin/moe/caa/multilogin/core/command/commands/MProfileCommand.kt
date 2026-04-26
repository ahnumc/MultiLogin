package moe.caa.multilogin.core.command.commands

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import moe.caa.multilogin.api.internal.plugin.IPlayer
import moe.caa.multilogin.api.internal.plugin.ISender
import moe.caa.multilogin.core.command.CommandHandler
import moe.caa.multilogin.core.command.Permissions
import moe.caa.multilogin.core.command.argument.OnlineArgumentType
import moe.caa.multilogin.core.command.argument.ProfileArgumentType
import moe.caa.multilogin.core.command.argument.ProfileArgumentType.ProfileArgument
import moe.caa.multilogin.core.command.argument.StringArgumentType
import moe.caa.multilogin.core.command.argument.UUIDArgumentType
import moe.caa.multilogin.core.command.submitConfirm
import java.sql.SQLException
import java.util.*
import java.util.regex.Pattern

class MProfileCommand(private val handler: CommandHandler) {
    fun register(literalArgumentBuilder: LiteralArgumentBuilder<ISender?>): LiteralArgumentBuilder<ISender?>? {
        return literalArgumentBuilder
            .then(
                handler.literal("create")
                    .requires { it?.hasPermission(Permissions.COMMAND_MULTI_LOGIN_PROFILE_CREATE) ?: false }
                    .then(
                        handler.argument("username", StringArgumentType.string())
                            .then(
                                handler.argument("ingameuuid", UUIDArgumentType.uuid())
                                    .executes { this.executeCreate(it) }
                            )
                            .executes { this.executeCreateRandomUUID(it) }
                    )
            )
            .then(
                handler.literal("set")
                    .then(
                        handler.argument("profile", ProfileArgumentType.profile())
                            .requires {
                                it?.hasPermission(Permissions.COMMAND_MULTI_LOGIN_PROFILE_SET_ONESELF) ?: false
                            }
                            .executes { this.executeSetOneself(it) }
                    )
                    .then(
                        handler.argument("profile", ProfileArgumentType.profile())
                            .then(
                                handler.argument("online", OnlineArgumentType.online())
                                    .requires {
                                        it?.hasPermission(Permissions.COMMAND_MULTI_LOGIN_PROFILE_SET_OTHER) ?: false
                                    }
                                    .executes { this.executeSetOther(it) }
                            )
                    )
            )
            .then(
                handler.literal("remove")
                    .then(
                        handler.argument("profile", ProfileArgumentType.profile())
                            .requires { it?.hasPermission(Permissions.COMMAND_MULTI_LOGIN_PROFILE_REMOVE) ?: false }
                            .executes { this.executeRemove(it) }
                    )
            )
    }

    private fun executeRemove(context: CommandContext<ISender?>): Int {
        val profile = ProfileArgumentType.getProfile(context, "profile")
        val sender = requireNotNull(context.source)
        val core = CommandHandler.core

        val name = profile.profileName
            ?: core.languageHandler.getMessage("command_message_profile_remove_unnamed")

        handler.submitConfirm(
            sender,
            "command_message_profile_remove_desc",
            "command_message_profile_remove_cq",
            "name" to name,
            "uuid" to profile.profileUUID
        ) {
            core.sqlManager.inGameProfileTable.remove(profile.profileUUID)
            sender.sendMessagePL(
                core.languageHandler.getMessage(
                    "command_message_profile_remove_succeed",
                    "name" to name,
                    "uuid" to profile.profileUUID
                )
            )

            val player: IPlayer? = core.plugin.runServer.playerManager.getPlayer(profile.profileUUID)
            player?.kickPlayer(
                core.languageHandler.getMessage("command_message_profile_remove_kickmessage")
            )
        }
        return 0
    }

    private fun executeSetOther(context: CommandContext<ISender?>): Int {
        val profile = ProfileArgumentType.getProfile(context, "profile")
        val online = OnlineArgumentType.getOnline(context, "online")
        processSet(
            context,
            online.onlineUUID,
            online.onlineName,
            online.baseServiceConfig.serviceId,
            profile
        )
        return 0
    }

    private fun executeSetOneself(context: CommandContext<ISender?>): Int {
        val profile = ProfileArgumentType.getProfile(context, "profile")
        val pair = handler.requireDataCacheArgumentSelf(context)

        processSet(context, pair.first?.id, pair.first?.name, requireNotNull(pair.second), profile)
        return 0
    }

    private fun processSet(
        context: CommandContext<ISender?>,
        from: UUID?,
        fromName: String?,
        serviceId: Int,
        to: ProfileArgument
    ) {
        val sender = requireNotNull(context.source)
        val core = CommandHandler.core
        handler.submitConfirm(
            sender,
            "command_message_profile_set_desc",
            "command_message_profile_set_cq",
            "redirect_name" to to.profileName,
            "redirect_uuid" to to.profileUUID,
            "online_uuid" to from,
            "online_name" to fromName
        ) {
            core.sqlManager.userDataTable.setInGameUUID(requireNotNull(from), serviceId, to.profileUUID)
            sender.sendMessagePL(
                core.languageHandler.getMessage(
                    "command_message_profile_set_succeed",
                    "redirect_name" to to.profileName,
                    "redirect_uuid" to to.profileUUID,
                    "online_uuid" to from,
                    "online_name" to fromName
                )
            )

            core.playerHandler.getInGameUUID(from, serviceId)?.let { inGameUUID ->
                core.plugin.runServer.playerManager.kickPlayerIfOnline(
                    inGameUUID,
                    core.languageHandler.getMessage(
                        "command_message_profile_set_succeed_kickmessage",
                        "redirect_name" to to.profileName,
                        "redirect_uuid" to to.profileUUID,
                        "online_uuid" to from,
                        "online_name" to fromName
                    )
                )
            }
        }
    }

    @Throws(SQLException::class)
    private fun processCreate(context: CommandContext<ISender?>, name: String, uuid: UUID) {
        val core = CommandHandler.core
        val sender = requireNotNull(context.source)
        val nameAllowedRegular = core.pluginConfig.nameAllowedRegular
        if (!nameAllowedRegular.isNullOrEmpty()) {
            if (!Pattern.matches(nameAllowedRegular, name)) {
                sender.sendMessagePL(
                    core.languageHandler.getMessage(
                        "command_message_profile_create_namemismatch",
                        "name" to name,
                        "regular" to nameAllowedRegular
                    )
                )
                return
            }
        }
        if (uuid.version() < 2) {
            sender.sendMessagePL(
                core.languageHandler.getMessage(
                    "command_message_profile_create_uuidmismatch",
                    "uuid" to uuid
                )
            )
            return
        }
        core.sqlManager.inGameProfileTable.get(uuid)?.let { pair ->
            sender.sendMessagePL(
                core.languageHandler.getMessage(
                    "command_message_profile_create_uuidoccupied",
                    "uuid" to uuid,
                    "name" to (pair.second ?: "")
                )
            )
            return
        }
        core.sqlManager.inGameProfileTable.getInGameUUIDIgnoreCase(name)?.let { uuidIgnoreCase ->
            sender.sendMessagePL(
                core.languageHandler.getMessage(
                    "command_message_profile_create_nameoccupied",
                    "name" to name,
                    "uuid" to uuidIgnoreCase
                )
            )
            return
        }
        core.sqlManager.inGameProfileTable.insertNewData(uuid, name)
        sender.sendMessagePL(
            core.languageHandler.getMessage(
                "command_message_profile_create",
                "uuid" to uuid,
                "name" to name
            )
        )
    }

    private fun executeCreate(context: CommandContext<ISender?>): Int {
        val username = StringArgumentType.getString(context, "username")
        val ingameuuid: UUID = UUIDArgumentType.getUuid(context, "ingameuuid")
        processCreate(context, username, ingameuuid)
        return 0
    }

    private fun executeCreateRandomUUID(context: CommandContext<ISender?>): Int {
        val username = StringArgumentType.getString(context, "username")
        val ingameuuid = UUID.randomUUID()
        processCreate(context, username, ingameuuid)
        return 0
    }
}
