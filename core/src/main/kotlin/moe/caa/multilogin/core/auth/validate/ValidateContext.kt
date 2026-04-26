package moe.caa.multilogin.core.auth.validate

import moe.caa.multilogin.api.profile.GameProfile
import moe.caa.multilogin.core.auth.service.BaseServiceAuthenticationResult

/**
 * 游戏内验证消息上下文
 */
class ValidateContext(val baseServiceAuthenticationResult: BaseServiceAuthenticationResult) {
    var inGameProfile: GameProfile =
        requireNotNull(baseServiceAuthenticationResult.response).copy()
        private set
    var disallowMessage: String = ""
        private set
    var needWait = false
        private set
    var onlineNameUpdated = false
        private set

    fun setInGameProfile(profile: GameProfile) { inGameProfile = profile }
    fun setDisallowMessage(msg: String) { disallowMessage = msg }
    fun setNeedWait(v: Boolean) { needWait = v }
    fun setOnlineNameUpdated(v: Boolean) { onlineNameUpdated = v }
}
