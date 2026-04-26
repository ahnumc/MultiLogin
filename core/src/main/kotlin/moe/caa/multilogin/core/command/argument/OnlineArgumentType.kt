package moe.caa.multilogin.core.command.argument

import com.mojang.brigadier.StringReader
import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.CommandSyntaxException
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import moe.caa.multilogin.api.internal.util.ValueUtil.getUuidOrNull
import moe.caa.multilogin.core.command.CommandHandler
import moe.caa.multilogin.core.command.UniversalCommandExceptionType
import moe.caa.multilogin.core.configuration.service.BaseServiceConfig
import java.util.*
import java.util.concurrent.CompletableFuture

/**
 * Online 参数阅读程序
 * <service_id> <online_uuid>|online_name>
 */
class OnlineArgumentType : ArgumentType<OnlineArgumentType.OnlineArgument> {
    @Throws(CommandSyntaxException::class)
    override fun parse(reader: StringReader): OnlineArgument {
        val i = reader.cursor
        val serviceConfig = ServiceIdArgumentType.readServiceConfig(reader)
        if (!reader.canRead()) {
            reader.cursor = i
            throw CommandHandler.builtInExceptions.dispatcherUnknownCommand().createWithContext(reader)
        }
        reader.skip()
        val nameOrUuid = StringArgumentType.readString(reader)

        val currentCore = CommandHandler.core
        val dataTable = currentCore.sqlManager.userDataTable

        val uuid = getUuidOrNull(nameOrUuid)
            ?: dataTable.getOnlineUUID(nameOrUuid, serviceConfig.serviceId)
            ?: run {
                reader.cursor = i
                throw UniversalCommandExceptionType.create(
                    currentCore.languageHandler.getMessage(
                        "command_message_online_not_found_by_name",
                        "service_name" to serviceConfig.serviceName,
                        "service_id" to serviceConfig.serviceId.toString(),
                        "online_name" to nameOrUuid
                    ), reader
                )
            }
        val there = dataTable.get(uuid, serviceConfig.serviceId) ?: run {
            reader.cursor = i
            throw UniversalCommandExceptionType.create(
                currentCore.languageHandler.getMessage(
                    "command_message_online_not_found_by_uuid",
                    "service_name" to serviceConfig.serviceName,
                    "service_id" to serviceConfig.serviceId.toString(),
                    "online_uuid" to uuid.toString()
                ), reader
            )
        }
        return OnlineArgument(serviceConfig, uuid, there.onlineName, there.inGameUUID, there.whitelist)
    }

    override fun <S> listSuggestions(
        context: CommandContext<S?>?,
        builder: SuggestionsBuilder
    ): CompletableFuture<Suggestions?>? {
        return ServiceIdArgumentType.getSuggestions(context, builder)
    }

    data class OnlineArgument(
        val baseServiceConfig: BaseServiceConfig,
        val onlineUUID: UUID,
        val onlineName: String?,
        val profileUUID: UUID?,
        val whitelist: Boolean
    )

    companion object {
        fun online(): OnlineArgumentType = OnlineArgumentType()

        fun getOnline(context: CommandContext<*>, name: String): OnlineArgument =
            context.getArgument(name, OnlineArgument::class.java)
    }
}
