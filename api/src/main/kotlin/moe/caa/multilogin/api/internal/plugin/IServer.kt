package moe.caa.multilogin.api.internal.plugin

import org.jetbrains.annotations.ApiStatus

@ApiStatus.Internal
interface IServer {
    val scheduler: BaseScheduler
    val playerManager: IPlayerManager
    val isOnlineMode: Boolean
    val isForwarded: Boolean
    val name: String
    val version: String
    fun shutdown()
    val consoleSender: ISender
    fun pluginHasEnabled(id: String): Boolean
}
