package moe.caa.multilogin.core.command.argument

import com.mojang.brigadier.StringReader
import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.context.CommandContext

/**
 * 修复的中文指令参数阅读程序
 */
class StringArgumentType : ArgumentType<String> {
    override fun parse(reader: StringReader): String {
        return readString(reader)
    }

    companion object {
        fun string(): StringArgumentType = StringArgumentType()

        fun getString(context: CommandContext<*>, name: String): String =
            context.getArgument(name, String::class.java)

        fun readString(reader: StringReader): String {
            val argBeginning = reader.getCursor()
            // 如果能读，并且下一个格子内容不是空
            while (reader.canRead() && reader.peek() != ' ') {
                // 游标++
                reader.skip()
            }
            return reader.getString().substring(argBeginning, reader.getCursor())
        }
    }
}
