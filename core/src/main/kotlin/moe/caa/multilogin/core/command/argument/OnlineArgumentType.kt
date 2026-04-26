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
        val serviceConfig: BaseServiceConfig = ServiceIdArgumentType.readServiceConfig(reader)
        if (!reader.canRead()) {
            reader.cursor = i
            throw CommandHandler.builtInExceptions.dispatcherUnknownCommand().createWithContext(reader)
        }
        reader.skip()
        val nameOrUuid: String = StringArgumentType.readString(reader)

        val currentCore = CommandHandler.core
        val dataTable = currentCore.sqlManager.userDataTable

        var uuid = getUuidOrNull(nameOrUuid)
        if (uuid == null) {
            uuid = dataTable.getOnlineUUID(nameOrUuid, serviceConfig.serviceId)
            if (uuid == null) {
                reader.cursor = i
                throw UniversalCommandExceptionType.create(
                    currentCore.languageHandler.getMessage(
                        "command_message_online_not_found_by_name",
                        "service_name" to serviceConfig.serviceName,
                        "service_id" to serviceConfig.serviceId,
                        "online_name" to nameOrUuid
                    ), reader
                )
            }
        }
        val there = dataTable.get(uuid, serviceConfig.serviceId)
        if (there == null) {
            reader.cursor = i
            throw UniversalCommandExceptionType.create(
                currentCore.languageHandler.getMessage(
                    "command_message_online_not_found_by_uuid",
                    "service_name" to serviceConfig.serviceName,
                    "service_id" to serviceConfig.serviceId,
                    "online_uuid" to uuid
                ), reader
            )
        }
        return OnlineArgument(serviceConfig, uuid, there.value1, there.value2, there.value3)
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
        val whitelist: Boolean?
    )

    companion object {
        fun online(): OnlineArgumentType {
            return OnlineArgumentType()
        }

        fun getOnline(context: CommandContext<*>, name: String?): OnlineArgument {
            return context.getArgument(name, OnlineArgument::class.java)
        }
    }
}
