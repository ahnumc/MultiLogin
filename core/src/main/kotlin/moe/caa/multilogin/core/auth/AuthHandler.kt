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
        try {
            val yggdrasilAuthenticationResult = yggdrasilAuthenticationService.hasJoined(username, serverId, ip)
            return when (yggdrasilAuthenticationResult.reason) {
                YggdrasilAuthenticationResult.Reason.NO_SERVICE ->
                    disallowByYggdrasil(yggdrasilAuthenticationResult, "auth_failed_no_yggdrasil_service")

                YggdrasilAuthenticationResult.Reason.SERVER_BREAKDOWN ->
                    disallowByYggdrasil(yggdrasilAuthenticationResult, "auth_yggdrasil_failed_server_down")

                YggdrasilAuthenticationResult.Reason.VALIDATION_FAILED ->
                    disallowByYggdrasil(yggdrasilAuthenticationResult, "auth_yggdrasil_failed_validation_failed")

                YggdrasilAuthenticationResult.Reason.ALLOWED -> {
                    if (yggdrasilAuthenticationResult.response == null
                        || yggdrasilAuthenticationResult.serviceConfig?.serviceId == -1
                    ) {
                        disallowByYggdrasil(yggdrasilAuthenticationResult, "auth_yggdrasil_failed_unknown")
                    } else {
                        checkIn(yggdrasilAuthenticationResult)
                    }
                }
            }
        } catch (e: Exception) {
            LoggerProvider.logger.error("An exception occurred while processing the hasJoined request.", e)
            return disallowByYggdrasil(null, "auth_yggdrasil_error")
        }
    }

    fun checkIn(baseServiceAuthenticationResult: BaseServiceAuthenticationResult): LoginAuthResult {
        try {
            val validateAuthenticationResult = validateAuthenticationService.checkIn(baseServiceAuthenticationResult)
            if (validateAuthenticationResult.reason != ValidateAuthenticationResult.Reason.ALLOWED) {
                return LoginAuthResult.ofDisallowedByValidateAuthenticator(
                    baseServiceAuthenticationResult,
                    validateAuthenticationResult,
                    validateAuthenticationResult.disallowedMessage
                )
            }

            val response = requireNotNull(baseServiceAuthenticationResult.response)
            val serviceConfig = requireNotNull(baseServiceAuthenticationResult.serviceConfig)
            val finalProfile = requireNotNull(validateAuthenticationResult.inGameProfile)

            LoggerProvider.logger.info(
                "%s(uuid: %s) from authentication service %s(sid: %d) has been authenticated, profile redirected to %s(uuid: %s).".format(
                    response.name,
                    response.id.toString(),
                    serviceConfig.serviceName,
                    serviceConfig.serviceId,
                    finalProfile.name,
                    finalProfile.id.toString()
                )
            )

            core.playerHandler.loginCache[finalProfile.id] = PlayerHandler.Entry(
                response,
                serviceConfig,
                System.currentTimeMillis()
            )
            return LoginAuthResult.ofAllowed(
                baseServiceAuthenticationResult,
                validateAuthenticationResult,
                finalProfile
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

    private fun disallowByYggdrasil(
        result: YggdrasilAuthenticationResult?,
        messageKey: String
    ): LoginAuthResult = LoginAuthResult.ofDisallowedByYggdrasilAuthenticator(
        result,
        core.languageHandler.getMessage(messageKey)
    )
}
