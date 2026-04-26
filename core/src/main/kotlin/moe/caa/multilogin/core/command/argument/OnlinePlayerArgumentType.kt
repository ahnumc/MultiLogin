package moe.caa.multilogin.core.command.argument

import com.mojang.brigadier.StringReader
import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.CommandSyntaxException
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import moe.caa.multilogin.api.internal.plugin.IPlayer
import moe.caa.multilogin.api.internal.util.ValueUtil.getUuidOrNull
import moe.caa.multilogin.core.command.CommandHandler
import moe.caa.multilogin.core.command.UniversalCommandExceptionType
import java.util.*
import java.util.concurrent.CompletableFuture

class OnlinePlayerArgumentType : ArgumentType<MutableSet<IPlayer>> {
    @Throws(CommandSyntaxException::class)
    override fun parse(reader: StringReader): MutableSet<IPlayer> {
        val i = reader.cursor
        val string: String = StringArgumentType.readString(reader)
        val core = CommandHandler.core

        val uuidOrNull = getUuidOrNull(string)
        if (uuidOrNull != null) {
            val player = core.plugin.runServer.playerManager.getPlayer(uuidOrNull)
            if (player == null) {
                reader.cursor = i
                throw UniversalCommandExceptionType.create(
                    core.languageHandler.getMessage(
                        "command_message_player_not_online_by_uuid",
                        "uuid" to string
                    ), reader
                )
            }
            return mutableSetOf(player)
        }
        val players = core.plugin.runServer.playerManager.getPlayers(string)
        if (players.isEmpty()) {
            reader.cursor = i
            throw UniversalCommandExceptionType.create(
                core.languageHandler.getMessage(
                    "command_message_player_not_online_by_name",
                    "name" to string
                ), reader
            )
        }
        return players
    }

    override fun <S> listSuggestions(
        context: CommandContext<S?>?,
        builder: SuggestionsBuilder
    ): CompletableFuture<Suggestions?>? {
        val core = CommandHandler.core
        for (key in core.plugin.runServer.playerManager.onlinePlayers) {
            if (key.name.lowercase(Locale.ROOT).startsWith(builder.remainingLowerCase)) builder.suggest(key.name)
        }
        return builder.buildFuture()
    }

    companion object {
        fun players(): OnlinePlayerArgumentType {
            return OnlinePlayerArgumentType()
        }

        fun getPlayers(context: CommandContext<*>, name: String?): MutableSet<IPlayer> {
            @Suppress("UNCHECKED_CAST")
            return context.getArgument(name, MutableSet::class.java) as MutableSet<IPlayer>
        }

        @Throws(CommandSyntaxException::class)
        fun getPlayer(context: CommandContext<*>, name: String?): IPlayer {
            val players = getPlayers(context, name)
            if (players.size == 1) {
                return players.iterator().next()
            }
            throw UniversalCommandExceptionType.create(
                CommandHandler.core.languageHandler.getMessage("command_message_player_multi_target")
            )
        }
    }
}
