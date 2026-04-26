package moe.caa.multilogin.api.internal.plugin

import org.jetbrains.annotations.ApiStatus
import java.net.SocketAddress
import java.util.*

@ApiStatus.Internal
interface IPlayer : ISender {
    fun kickPlayer(message: String?)
    val uniqueId: UUID
    val address: SocketAddress
    val isOnline: Boolean
}
