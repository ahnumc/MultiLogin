package moe.caa.multilogin.core.command.commands

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import moe.caa.multilogin.api.internal.plugin.ISender
import moe.caa.multilogin.core.command.CommandHandler
import moe.caa.multilogin.core.command.Permissions

class MDataConvert(private val handler: CommandHandler) {
    fun register(builder: LiteralArgumentBuilder<ISender?>): LiteralArgumentBuilder<ISender?>? {
        return builder.requires { it?.hasPermission(Permissions.COMMAND_MULTI_LOGIN_DATA_CONVERT) ?: false }
            .then(
                handler.literal("fromFloodgateOwnLinkData")
                    .then(handler.literal("sqlite"))
                    .then(handler.literal("mysql"))
            )
    }
}
