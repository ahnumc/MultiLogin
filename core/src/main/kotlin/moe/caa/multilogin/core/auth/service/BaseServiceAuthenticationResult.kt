package moe.caa.multilogin.core.auth.service

import moe.caa.multilogin.api.profile.GameProfile
import moe.caa.multilogin.core.configuration.service.BaseServiceConfig

abstract class BaseServiceAuthenticationResult(
    val response: GameProfile?,
    val serviceConfig: BaseServiceConfig?
) {
    abstract val isAllowed: Boolean
}
