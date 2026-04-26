package moe.caa.multilogin.api.internal.plugin

import org.jetbrains.annotations.ApiStatus

/**
 * 公共命令执行者对象
 */
@ApiStatus.Internal
interface ISender {
    val isPlayer: Boolean
    val isConsole: Boolean
    fun hasPermission(permission: String): Boolean
    fun sendMessagePL(message: String)
    val name: String
    val asPlayer: IPlayer?
}
