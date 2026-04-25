package moe.caa.multilogin.core.auth

import moe.caa.multilogin.api.internal.auth.AuthAPI
import moe.caa.multilogin.api.internal.logger.LoggerProvider
import moe.caa.multilogin.core.auth.service.BaseServiceAuthenticationResult
import moe.caa.multilogin.core.auth.service.yggdrasil.YggdrasilAuthenticationResult
import moe.caa.multilogin.core.auth.service.yggdrasil.YggdrasilAuthenticationService
import moe.caa.multilogin.core.auth.validate.ValidateAuthenticationResult
import moe.caa.multilogin.core.auth.validate.ValidateAuthenticationService
import moe.caa.multilogin.core.handle.PlayerHandler
import moe.caa.multilogin.core.main.MultiCore

class AuthHandler(private val core: MultiCore) : AuthAPI {
    val yggdrasilAuthenticationService: YggdrasilAuthenticationService = YggdrasilAuthenticationService(core)
    val validateAuthenticationService: ValidateAuthenticationService = ValidateAuthenticationService(core)

    override fun auth(username: String, serverId: String, ip: String): LoginAuthResult {
        val yggdrasilAuthenticationResult: YggdrasilAuthenticationResult
        try {
            yggdrasilAuthenticationResult = yggdrasilAuthenticationService.hasJoined(username, serverId, ip)
            if (yggdrasilAuthenticationResult.reason == YggdrasilAuthenticationResult.Reason.NO_SERVICE) {
                return LoginAuthResult.ofDisallowedByYggdrasilAuthenticator(
                    yggdrasilAuthenticationResult,
                    core.languageHandler.getMessage("auth_failed_no_yggdrasil_service")
                )
            }
            if (yggdrasilAuthenticationResult.reason == YggdrasilAuthenticationResult.Reason.SERVER_BREAKDOWN) {
                return LoginAuthResult.ofDisallowedByYggdrasilAuthenticator(
                    yggdrasilAuthenticationResult,
                    core.languageHandler.getMessage("auth_yggdrasil_failed_server_down")
                )
            }
            if (yggdrasilAuthenticationResult.reason == YggdrasilAuthenticationResult.Reason.VALIDATION_FAILED) {
                return LoginAuthResult.ofDisallowedByYggdrasilAuthenticator(
                    yggdrasilAuthenticationResult,
                    core.languageHandler.getMessage("auth_yggdrasil_failed_validation_failed")
                )
            }
            if (yggdrasilAuthenticationResult.reason != YggdrasilAuthenticationResult.Reason.ALLOWED
                || yggdrasilAuthenticationResult.response == null
                || yggdrasilAuthenticationResult.serviceConfig?.serviceId == -1
            ) {
                return LoginAuthResult.ofDisallowedByYggdrasilAuthenticator(
                    yggdrasilAuthenticationResult,
                    core.languageHandler.getMessage("auth_yggdrasil_failed_unknown")
                )
            }
        } catch (e: Exception) {
            LoggerProvider.logger.error("An exception occurred while processing the hasJoined request.", e)
            return LoginAuthResult.ofDisallowedByYggdrasilAuthenticator(
                null,
                core.languageHandler.getMessage("auth_yggdrasil_error")
            )
        }

        return checkIn(yggdrasilAuthenticationResult)
    }

    fun checkIn(baseServiceAuthenticationResult: BaseServiceAuthenticationResult): LoginAuthResult {
        try {
            val validateAuthenticationResult = validateAuthenticationService.checkIn(baseServiceAuthenticationResult)
            if (validateAuthenticationResult.reason == ValidateAuthenticationResult.Reason.ALLOWED) {
                val response = requireNotNull(baseServiceAuthenticationResult.response)
                val serviceConfig = requireNotNull(baseServiceAuthenticationResult.serviceConfig)
                LoggerProvider.logger.info(
                    "%s(uuid: %s) from authentication service %s(sid: %d) has been authenticated, profile redirected to %s(uuid: %s).".format(
                        response.name,
                        response.id.toString(),
                        serviceConfig.serviceName,
                        serviceConfig.serviceId,
                        validateAuthenticationResult.inGameProfile?.name,
                        validateAuthenticationResult.inGameProfile?.id.toString()
                    )
                )
                val finalProfile = requireNotNull(validateAuthenticationResult.inGameProfile)
                core.playerHandler.loginCache[requireNotNull(finalProfile.id)] = PlayerHandler.Entry(
                    response,
                    serviceConfig,
                    System.currentTimeMillis()
                )
                return LoginAuthResult.ofAllowed(
                    baseServiceAuthenticationResult,
                    validateAuthenticationResult,
                    finalProfile
                )
            }
            return LoginAuthResult.ofDisallowedByValidateAuthenticator(
                baseServiceAuthenticationResult,
                validateAuthenticationResult,
                validateAuthenticationResult.disallowedMessage
            )
        } catch (e: Exception) {
            LoggerProvider.logger.error("An exception occurred while processing the validation request.", e)
            return LoginAuthResult.ofDisallowedByValidateAuthenticator(
                baseServiceAuthenticationResult,
                null,
                core.languageHandler.getMessage("auth_validate_error")
            )
        }
    }
}
