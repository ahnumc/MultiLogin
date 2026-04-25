package moe.caa.multilogin.core.auth.validate

import moe.caa.multilogin.api.profile.GameProfile

/**
 * 游戏内验证结果
 */
class ValidateAuthenticationResult private constructor(
    val reason: Reason,
    val inGameProfile: GameProfile?,
    val disallowedMessage: String?
) {
    enum class Reason {
        ALLOWED,
        DISALLOWED
    }

    companion object {
        fun ofAllowed(response: GameProfile): ValidateAuthenticationResult {
            return ValidateAuthenticationResult(Reason.ALLOWED, response, null)
        }

        fun ofDisallowed(disallowedMessage: String): ValidateAuthenticationResult {
            return ValidateAuthenticationResult(Reason.DISALLOWED, null, disallowedMessage)
        }
    }
}
