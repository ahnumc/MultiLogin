package moe.caa.multilogin.core.command.argument

import com.mojang.brigadier.StringReader
import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.CommandSyntaxException
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import moe.caa.multilogin.api.internal.util.Pair
import moe.caa.multilogin.api.internal.util.ValueUtil.getUuidOrNull
import moe.caa.multilogin.core.command.CommandHandler
import moe.caa.multilogin.core.command.UniversalCommandExceptionType
import java.util.*
import java.util.concurrent.CompletableFuture

/**
 * Profile 参数阅读程序
 * <profile_name>|profile_uuid>
 */
class ProfileArgumentType : ArgumentType<ProfileArgumentType.ProfileArgument> {
    @Throws(CommandSyntaxException::class)
    override fun parse(reader: StringReader): ProfileArgument {
        val i = reader.cursor

        val nameOrUuid: String = StringArgumentType.readString(reader)

        val table = CommandHandler.core!!.sqlManager.inGameProfileTable!!

        var uuid = getUuidOrNull(nameOrUuid)
        if (uuid == null) {
            uuid = table.getInGameUUIDIgnoreCase(nameOrUuid)
            if (uuid == null) {
                reader.cursor = i
                throw UniversalCommandExceptionType.create(
                    CommandHandler.core!!.languageHandler.getMessage(
                        "command_message_profile_not_found_by_name",
                        Pair<Any?, Any?>("name", nameOrUuid)
                    ), reader
                )
            }

            return ProfileArgument(uuid, table.getUsername(uuid))
        }
        val username = table.getUsername(uuid)
        if (username == null) {
            reader.cursor = i
            throw UniversalCommandExceptionType.create(
                CommandHandler.core!!.languageHandler.getMessage(
                    "command_message_profile_not_found_by_uuid",
                    Pair<Any?, Any?>("uuid", uuid)
                ), reader
            )
        }
        return ProfileArgument(uuid, username)
    }

    data class ProfileArgument(
        val profileUUID: UUID,
        val profileName: String?
    )

    override fun <S> listSuggestions(
        context: CommandContext<S?>?,
        builder: SuggestionsBuilder
    ): CompletableFuture<Suggestions?>? {
        for (key in CommandHandler.core!!.plugin.runServer.playerManager.onlinePlayers) {
            if (key.name.lowercase(Locale.ROOT).startsWith(builder.remainingLowerCase)) builder.suggest(key.name)
        }
        return builder.buildFuture()
    }

    companion object {
        fun profile(): ProfileArgumentType {
            return ProfileArgumentType()
        }

        fun getProfile(context: CommandContext<*>, name: String?): ProfileArgument {
            return context.getArgument(name, ProfileArgument::class.java)
        }
    }
}
