package moe.caa.multilogin.core.command.commands

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import moe.caa.multilogin.api.internal.plugin.ISender
import moe.caa.multilogin.api.internal.util.ValueUtil.isEmpty
import moe.caa.multilogin.core.command.CommandHandler
import moe.caa.multilogin.core.command.Permissions
import moe.caa.multilogin.core.command.argument.ProfileArgumentType
import moe.caa.multilogin.core.command.argument.ProfileArgumentType.ProfileArgument
import moe.caa.multilogin.core.command.argument.StringArgumentType
import moe.caa.multilogin.core.command.submitConfirm
import java.sql.SQLIntegrityConstraintViolationException
import java.util.regex.Pattern

class MRenameCommand(private val handler: CommandHandler) {
    fun register(literalArgumentBuilder: LiteralArgumentBuilder<ISender?>): LiteralArgumentBuilder<ISender?>? {
        return literalArgumentBuilder
            .then(
                handler.argument("newname", StringArgumentType.string())
                    .requires { it?.hasPermission(Permissions.COMMAND_MULTILOGIN_RENAME_ONESELF) ?: false }
                    .executes { this.executeRename(it) }
            )
            .then(
                handler.argument("newname", StringArgumentType.string())
                    .then(
                        handler.argument("profile", ProfileArgumentType.profile())
                            .requires { it?.hasPermission(Permissions.COMMAND_MULTILOGIN_RENAME_OTHER) ?: false }
                            .executes { this.executeRenameOther(it) }
                    )
            )
    }

    private fun executeRenameOther(context: CommandContext<ISender?>): Int {
        val newname = StringArgumentType.getString(context, "newname")
        val profile = ProfileArgumentType.getProfile(context, "profile")

        processRename(context, newname, profile)
        return 0
    }

    private fun executeRename(context: CommandContext<ISender?>): Int {
        val newname = StringArgumentType.getString(context, "newname")
        handler.requireDataCacheArgumentSelf(context)
        val player = requireNotNull(context.source?.asPlayer)

        processRename(
            context,
            newname,
            ProfileArgument(player.uniqueId, player.name)
        )
        return 0
    }

    private fun processRename(context: CommandContext<ISender?>, newName: String, argument: ProfileArgument) {
        val sender = requireNotNull(context.source)
        val core = CommandHandler.core
        if (newName == argument.profileName) {
            sender.sendMessagePL(core.languageHandler.getMessage("command_message_rename_identical"))
            return
        }
        val nameAllowedRegular = core.pluginConfig.nameAllowedRegular
        if (!isEmpty(nameAllowedRegular)) {
            if (!Pattern.matches(nameAllowedRegular, newName)) {
                sender.sendMessagePL(
                    core.languageHandler.getMessage(
                        "command_message_rename_mismatch",
                        "name" to newName,
                        "regular" to nameAllowedRegular
                    )
                )
                return
            }
        }

        handler.submitConfirm(
            sender,
            "command_message_rename_desc",
            "command_message_rename_cq",
            "profile_name" to argument.profileName,
            "new_name" to newName,
            "profile_uuid" to argument.profileUUID
        ) {
            try {
                core.sqlManager.inGameProfileTable.updateUsername(argument.profileUUID, newName)
                sender.sendMessagePL(
                    core.languageHandler.getMessage(
                        "command_message_rename_succeed",
                        "profile_name" to argument.profileName,
                        "new_name" to newName,
                        "profile_uuid" to argument.profileUUID
                    )
                )

                core.plugin.runServer.playerManager.kickPlayerIfOnline(
                    argument.profileUUID, core.languageHandler.getMessage(
                        "command_message_rename_succeed_kickmessage",
                        "profile_name" to argument.profileName,
                        "new_name" to newName,
                        "profile_uuid" to argument.profileUUID
                    )
                )
            } catch (e: SQLIntegrityConstraintViolationException) {
                sender.sendMessagePL(
                    core.languageHandler.getMessage(
                        "command_message_rename_occupied",
                        "name" to newName
                    )
                )
            }
        }
    }
}
