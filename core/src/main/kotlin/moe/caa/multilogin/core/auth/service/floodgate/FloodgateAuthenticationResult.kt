package moe.caa.multilogin.core.auth.service.floodgate

import moe.caa.multilogin.api.profile.GameProfile
import moe.caa.multilogin.core.auth.service.BaseServiceAuthenticationResult
import moe.caa.multilogin.core.configuration.service.FloodgateServiceConfig

class FloodgateAuthenticationResult(response: GameProfile?, serviceConfig: FloodgateServiceConfig?) :
    BaseServiceAuthenticationResult(response, serviceConfig) {
    override val isAllowed: Boolean get() = true
}
