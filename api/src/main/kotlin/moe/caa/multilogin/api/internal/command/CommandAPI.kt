package moe.caa.multilogin.api.internal.command

import moe.caa.multilogin.api.internal.plugin.ISender
import org.jetbrains.annotations.ApiStatus

/**
 * 命令处理程序
 */
@ApiStatus.Internal
interface CommandAPI {
    /**
     * 执行一条指令
     * 
     * @param sender 指令发送者
     * @param args   指令参数
     */
    fun execute(sender: ISender, args: Array<String>)

    fun execute(sender: ISender, args: String)

    /**
     * 执行指令建议补全
     * 
     * @param sender 指令发送者
     * @param args   指令参数
     */
    fun tabComplete(sender: ISender, args: Array<String>): MutableList<String>

    fun tabComplete(sender: ISender, args: String): MutableList<String>
}
