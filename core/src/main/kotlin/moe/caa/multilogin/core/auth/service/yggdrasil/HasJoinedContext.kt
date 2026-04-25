package moe.caa.multilogin.core.auth.service.yggdrasil

import moe.caa.multilogin.api.internal.util.Pair
import moe.caa.multilogin.api.profile.GameProfile
import moe.caa.multilogin.core.configuration.service.yggdrasil.BaseYggdrasilServiceConfig
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference

/**
 * HasJoined 验证上下文
 */
class HasJoinedContext(val username: String?, val serverId: String?, val ip: String?) {
    val response = AtomicReference<Pair<GameProfile?, BaseYggdrasilServiceConfig?>?>()
    val serviceUnavailable: MutableMap<BaseYggdrasilServiceConfig?, Throwable?> = ConcurrentHashMap()
    val authenticationFailed: MutableSet<Int?> = ConcurrentHashMap.newKeySet()
}
