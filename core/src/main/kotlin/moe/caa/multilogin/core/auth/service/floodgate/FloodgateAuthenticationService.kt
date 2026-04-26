package moe.caa.multilogin.core.auth.service.floodgate

import moe.caa.multilogin.api.internal.auth.AuthResult
import moe.caa.multilogin.api.internal.util.ValueUtil.xuidToUUID
import moe.caa.multilogin.api.profile.GameProfile
import moe.caa.multilogin.core.auth.LoginAuthResult
import moe.caa.multilogin.core.main.MultiCore
import org.geysermc.event.PostOrder
import org.geysermc.event.subscribe.Subscribe
import org.geysermc.floodgate.api.FloodgateApi
import org.geysermc.floodgate.api.InstanceHolder
import org.geysermc.floodgate.api.event.skin.SkinApplyEvent
import org.geysermc.floodgate.api.handshake.HandshakeData
import org.geysermc.floodgate.api.handshake.HandshakeHandler
import org.geysermc.floodgate.util.LinkedPlayer

class FloodgateAuthenticationService(private val multiCore: MultiCore) : HandshakeHandler {
    @Subscribe(postOrder = PostOrder.FIRST)
    fun onSkinApply(event: SkinApplyEvent) {
        if (!multiCore.pluginConfig.floodgateSupport) return

        // always apply bedrock skin.
        event.isCancelled = false
    }

    @Subscribe(ignoreCancelled = true, postOrder = PostOrder.FIRST)
    fun onSkinApplyIgnoreCancelled(event: SkinApplyEvent) {
        onSkinApply(event)
    }

    fun register() {
        InstanceHolder.getHandshakeHandlers().addHandshakeHandler(this)
        FloodgateApi.getInstance().eventBus.register(this)
    }

    override fun handle(handshakeData: HandshakeData) {
        if (!multiCore.pluginConfig.floodgateSupport) {
            return
        }
        val data = handshakeData.bedrockData
        val xuid = data.xuid
        val uuid = xuidToUUID(xuid)
        val profile: GameProfile = GameProfile(
            uuid,
            initBedrockUsername(handshakeData.bedrockData.username),
            mutableMapOf()
        )
        val service = multiCore.pluginConfig.floodgateAuthenticationService ?: run {
            handshakeData.setDisconnectReason(
                multiCore.languageHandler.getMessage("auth_floodgate_service_notfound")
            )
            return
        }
        val result = FloodgateAuthenticationResult(profile, service)
        val loginAuthResult: LoginAuthResult = multiCore.authHandler.checkIn(result)
        if (loginAuthResult.result == AuthResult.Result.ALLOW) {
            val gameProfile: GameProfile = requireNotNull(loginAuthResult.response)
            handshakeData.linkedPlayer = LinkedPlayer.of(gameProfile.name, gameProfile.id, uuid)
        } else {
            handshakeData.disconnectReason = loginAuthResult.kickMessage
        }
    }

    private fun initBedrockUsername(bedrockUsername: String): String =
        bedrockUsername.map { if (isNameAllowedCharacter(it)) it else '_' }.joinToString("")

    private fun isNameAllowedCharacter(c: Char): Boolean {
        return (c in 'a'..'z') || (c in '0'..'9') || (c in 'A'..'Z') || c == '_'
    }
}
