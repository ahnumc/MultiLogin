package moe.caa.multilogin.core.command.argument

import com.mojang.brigadier.StringReader
import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.CommandSyntaxException
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import moe.caa.multilogin.core.command.CommandHandler
import moe.caa.multilogin.core.command.UniversalCommandExceptionType
import moe.caa.multilogin.core.configuration.service.BaseServiceConfig
import java.util.*
import java.util.concurrent.CompletableFuture

/**
 * Service 参数阅读程序
 */
class ServiceIdArgumentType private constructor() : ArgumentType<BaseServiceConfig> {
    @Throws(CommandSyntaxException::class)
    override fun parse(reader: StringReader): BaseServiceConfig {
        return readServiceConfig(reader)
    }

    override fun <S> listSuggestions(
        context: CommandContext<S?>?,
        builder: SuggestionsBuilder
    ): CompletableFuture<Suggestions?>? {
        return getSuggestions(context, builder)
    }

    companion object {
        fun service(): ServiceIdArgumentType {
            return ServiceIdArgumentType()
        }

        fun getService(context: CommandContext<*>, name: String?): BaseServiceConfig {
            return context.getArgument(name, BaseServiceConfig::class.java)
        }

        @Throws(CommandSyntaxException::class)
        fun readServiceConfig(reader: StringReader): BaseServiceConfig {
            val start = reader.cursor
            val result = reader.readInt()
            val currentCore = CommandHandler.core
            val config = currentCore.pluginConfig.serviceIdMap[result]
            if (config == null) {
                reader.cursor = start
                throw UniversalCommandExceptionType.create(
                    currentCore.languageHandler.getMessage(
                        "command_exception_serviceid_not_found",
                        "service_id" to result
                    ), reader
                )
            }
            return config
        }

        fun <S> getSuggestions(
            context: CommandContext<S?>?,
            builder: SuggestionsBuilder
        ): CompletableFuture<Suggestions?>? {
            CommandHandler.core.pluginConfig.serviceIdMap
                .forEach { (key: Int?, value: BaseServiceConfig?) ->
                    if (key.toString().startsWith(builder.remaining.lowercase(Locale.getDefault()))) {
                        builder.suggest(requireNotNull(key))
                    }
                }
            return builder.buildFuture()
        }
    }
}
