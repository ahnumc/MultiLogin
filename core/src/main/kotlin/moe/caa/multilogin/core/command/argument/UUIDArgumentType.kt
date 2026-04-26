package moe.caa.multilogin.core.command.argument

import com.mojang.brigadier.StringReader
import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.CommandSyntaxException
import moe.caa.multilogin.api.internal.util.ValueUtil.getUuidOrNull
import moe.caa.multilogin.core.command.CommandHandler
import moe.caa.multilogin.core.command.UniversalCommandExceptionType
import java.util.*

/**
 * UUID 参数阅读程序
 */
class UUIDArgumentType private constructor() : ArgumentType<UUID> {
    @Throws(CommandSyntaxException::class)
    override fun parse(reader: StringReader): UUID {
        val argBeginning = reader.cursor

        val uuidString: String = StringArgumentType.readString(reader)
        val ret = getUuidOrNull(uuidString)
        if (ret == null) {
            reader.cursor = argBeginning
            throw UniversalCommandExceptionType.create(
                CommandHandler.core.languageHandler.getMessage(
                    "command_exception_reader_invalid_uuid",
                    "value" to uuidString
                ), reader
            )
        }
        return ret
    }

    companion object {
        fun uuid(): UUIDArgumentType {
            return UUIDArgumentType()
        }

        fun getUuid(context: CommandContext<*>, name: String?): UUID {
            return context.getArgument(name, UUID::class.java)
        }
    }
}
