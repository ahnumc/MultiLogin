package moe.caa.multilogin.core.auth.service.yggdrasil

import moe.caa.multilogin.api.profile.GameProfile
import moe.caa.multilogin.core.auth.service.BaseServiceAuthenticationResult
import moe.caa.multilogin.core.configuration.service.yggdrasil.BaseYggdrasilServiceConfig

/**
 * HasJoined 验证结果
 */
class YggdrasilAuthenticationResult(
    val reason: Reason,
    response: GameProfile?,
    serviceConfig: BaseYggdrasilServiceConfig?
) : BaseServiceAuthenticationResult(response, serviceConfig) {

    override val isAllowed: Boolean get() = reason == Reason.ALLOWED

    enum class Reason {
        ALLOWED,
        SERVER_BREAKDOWN,
        VALIDATION_FAILED,
        NO_SERVICE
    }

    companion object {
        fun ofAllowed(response: GameProfile?, serviceConfig: BaseYggdrasilServiceConfig?): YggdrasilAuthenticationResult {
            return YggdrasilAuthenticationResult(Reason.ALLOWED, response, serviceConfig)
        }

        fun ofServerBreakdown(): YggdrasilAuthenticationResult {
            return YggdrasilAuthenticationResult(Reason.SERVER_BREAKDOWN, null, null)
        }

        fun ofValidationFailed(): YggdrasilAuthenticationResult {
            return YggdrasilAuthenticationResult(Reason.VALIDATION_FAILED, null, null)
        }

        fun ofNoService(): YggdrasilAuthenticationResult {
            return YggdrasilAuthenticationResult(Reason.NO_SERVICE, null, null)
        }
    }
}
