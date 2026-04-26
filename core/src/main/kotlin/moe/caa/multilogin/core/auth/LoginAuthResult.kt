package moe.caa.multilogin.core.auth

import moe.caa.multilogin.api.internal.auth.AuthResult
import moe.caa.multilogin.api.profile.GameProfile
import moe.caa.multilogin.core.auth.service.BaseServiceAuthenticationResult
import moe.caa.multilogin.core.auth.service.yggdrasil.YggdrasilAuthenticationResult
import moe.caa.multilogin.core.auth.validate.ValidateAuthenticationResult

open class LoginAuthResult protected constructor(
    override val response: GameProfile?,
    override val kickMessage: String?,
    override val result: AuthResult.Result?,
    val baseServiceAuthenticationResult: BaseServiceAuthenticationResult?,
    val validateAuthenticationResult: ValidateAuthenticationResult?
) : AuthResult {
    companion object {
        fun ofDisallowedByYggdrasilAuthenticator(
            yggdrasilAuthenticationResult: YggdrasilAuthenticationResult?,
            kickMessage: String?
        ): LoginAuthResult {
            return LoginAuthResult(
                null, kickMessage,
                AuthResult.Result.DISALLOW_BY_YGGDRASIL_AUTHENTICATOR,
                yggdrasilAuthenticationResult, null
            )
        }

        fun ofDisallowedByValidateAuthenticator(
            baseServiceAuthenticationResult: BaseServiceAuthenticationResult?,
            validateAuthenticationResult: ValidateAuthenticationResult?,
            kickMessage: String?
        ): LoginAuthResult {
            return LoginAuthResult(
                null, kickMessage,
                AuthResult.Result.DISALLOW_BY_VALIDATE_AUTHENTICATOR,
                baseServiceAuthenticationResult, validateAuthenticationResult
            )
        }

        fun ofAllowed(
            baseServiceAuthenticationResult: BaseServiceAuthenticationResult?,
            validateAuthenticationResult: ValidateAuthenticationResult?,
            gameProfile: GameProfile
        ): LoginAuthResult {
            return LoginAuthResult(
                gameProfile.copy(),
                null,
                AuthResult.Result.ALLOW,
                baseServiceAuthenticationResult, validateAuthenticationResult
            )
        }
    }
}
