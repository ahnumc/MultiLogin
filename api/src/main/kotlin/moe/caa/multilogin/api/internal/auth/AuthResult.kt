package moe.caa.multilogin.api.internal.auth

import moe.caa.multilogin.api.profile.GameProfile
import org.jetbrains.annotations.ApiStatus

/**
 * 验证结果
 */
@ApiStatus.Internal
interface AuthResult {
    val response: GameProfile?
    val kickMessage: String?
    val result: Result?

    enum class Result {
        ALLOW,
        DISALLOW_BY_YGGDRASIL_AUTHENTICATOR,
        DISALLOW_BY_VALIDATE_AUTHENTICATOR,
        ERROR
    }
}
