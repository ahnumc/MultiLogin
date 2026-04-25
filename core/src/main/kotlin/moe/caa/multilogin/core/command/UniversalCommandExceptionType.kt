package moe.caa.multilogin.core.command

import com.mojang.brigadier.ImmutableStringReader
import com.mojang.brigadier.LiteralMessage
import com.mojang.brigadier.Message
import com.mojang.brigadier.exceptions.CommandExceptionType
import com.mojang.brigadier.exceptions.CommandSyntaxException

object UniversalCommandExceptionType : CommandExceptionType {
    fun create(message: Message): CommandSyntaxException {
        return CommandSyntaxException(this, message)
    }

    fun create(message: String?): CommandSyntaxException {
        return create(LiteralMessage(message))
    }

    fun create(message: Message, reader: ImmutableStringReader): CommandSyntaxException {
        return CommandSyntaxException(this, message, reader.string, reader.cursor)
    }

    fun create(message: String?, reader: ImmutableStringReader): CommandSyntaxException {
        return CommandSyntaxException(this, LiteralMessage(message), reader.string, reader.cursor)
    }
}
