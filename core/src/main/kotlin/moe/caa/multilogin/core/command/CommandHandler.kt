package moe.caa.multilogin.core.command

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.builder.RequiredArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.CommandSyntaxException
import moe.caa.multilogin.api.internal.command.CommandAPI
import moe.caa.multilogin.api.internal.logger.LoggerProvider
import moe.caa.multilogin.api.internal.plugin.IPlayer
import moe.caa.multilogin.api.internal.plugin.ISender
import moe.caa.multilogin.api.internal.util.Pair
import moe.caa.multilogin.api.profile.GameProfile
import moe.caa.multilogin.core.command.commands.RootCommand
import moe.caa.multilogin.core.main.MultiCore

/**
 * 中央命令处理程序
 */
class CommandHandler(core: MultiCore) : CommandAPI {
    private val dispatcher: CommandDispatcher<ISender?>
    val secondaryConfirmationHandler: SecondaryConfirmationHandler

    init {
        Companion.core = core
        this.dispatcher = CommandDispatcher<ISender?>()
        this.secondaryConfirmationHandler = SecondaryConfirmationHandler()
    }

    fun init() {
        dispatcher.register(RootCommand(this).register(literal("multilogin")))
        builtInExceptions = BuiltInExceptions(core)
        CommandSyntaxException.BUILT_IN_EXCEPTIONS = builtInExceptions
    }

    override fun execute(sender: ISender, args: Array<String>) {
        execute(sender, args.joinToString(" "))
    }

    override fun execute(sender: ISender, args: String) {
        val currentCore = core
        currentCore.plugin.runServer.scheduler.runTaskAsync({
            try {
                dispatcher.execute(args, sender)
            } catch (e: CommandSyntaxException) {
                sender.sendMessagePL(e.rawMessage.string)
                LoggerProvider.logger.debug(
                    "An expected exception occurs when the %s command is executed.".format(args), e
                )
            } catch (e: Exception) {
                sender.sendMessagePL(currentCore.languageHandler.getMessage("command_error"))
                LoggerProvider.logger.error(
                    "An exception occurs when the %s command is executed.".format(args), e
                )
            }
        })
    }

    override fun tabComplete(sender: ISender, args: Array<String>): MutableList<String> {
        if (args.size == 1) {
            return tabComplete(sender, args[0] + " ")
        }
        return tabComplete(sender, args.joinToString(" "))
    }

    override fun tabComplete(sender: ISender, args: String): MutableList<String> {
        if (!sender.hasPermission(Permissions.COMMAND_TAB_COMPLETE)) {
            return mutableListOf()
        }
        val suggestions = dispatcher.getCompletionSuggestions(dispatcher.parse(args, sender))
        return try {
            suggestions.get().list.map { it.text }.toMutableList()
        } catch (e: Exception) {
            LoggerProvider.logger.error("An exception occurred while executing the $args command to complete.", e)
            mutableListOf()
        }
    }

    fun literal(literal: String?): LiteralArgumentBuilder<ISender?> {
        return LiteralArgumentBuilder.literal<ISender?>(literal)
    }

    fun <T> argument(name: String?, type: ArgumentType<T>?): RequiredArgumentBuilder<ISender?, T> {
        return RequiredArgumentBuilder.argument<ISender?, T>(name, type)
    }

    @Throws(CommandSyntaxException::class)
    fun requirePlayer(context: CommandContext<ISender?>) {
        val sender = requireNotNull(context.source)
        if (!sender.isPlayer) {
            throw builtInExceptions.requirePlayer().create()
        }
    }

    @Throws(CommandSyntaxException::class)
    fun requirePlayerAndNoSelf(context: CommandContext<ISender?>, player: IPlayer) {
        val sender = requireNotNull(context.source)
        if (!sender.isPlayer) {
            throw builtInExceptions.requirePlayer().create()
        }
        if (requireNotNull(sender.asPlayer).uniqueId == player.uniqueId) {
            throw builtInExceptions.noSelf().create()
        }
    }

    @Throws(CommandSyntaxException::class)
    fun requireDataCacheArgumentSelf(context: CommandContext<ISender?>): Pair<GameProfile?, Int?> {
        requirePlayer(context)
        val currentCore = core
        val player = requireNotNull(requireNotNull(context.source).asPlayer)
        return currentCore.playerHandler.getPlayerOnlineProfile(player.uniqueId)
            ?: throw builtInExceptions.cacheNotFoundSelf().create()
    }

    @Throws(CommandSyntaxException::class)
    fun requireDataCacheArgumentOther(player: IPlayer): Pair<GameProfile?, Int?> =
        core.playerHandler.getPlayerOnlineProfile(player.uniqueId)
            ?: throw builtInExceptions.cacheNotFoundOther().create(player.uniqueId, player.name)

    companion object {
        lateinit var core: MultiCore
            private set
        lateinit var builtInExceptions: BuiltInExceptions
            private set
    }
}

fun CommandHandler.submitConfirm(
    sender: ISender,
    descKey: String,
    cqKey: String,
    vararg args: Pair<Any?, Any?>,
    action: () -> Unit
) {
    val currentCore = CommandHandler.core
    secondaryConfirmationHandler.submit(
        sender,
        object : SecondaryConfirmationHandler.CallbackConfirmCommand { override fun confirm() = action() },
        currentCore.languageHandler.getMessage(descKey, *args),
        currentCore.languageHandler.getMessage(cqKey, *args)
    )
}
