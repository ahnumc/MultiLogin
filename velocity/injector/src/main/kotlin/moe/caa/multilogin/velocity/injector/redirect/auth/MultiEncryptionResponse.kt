package moe.caa.multilogin.velocity.injector.redirect.auth

import com.velocitypowered.proxy.connection.MinecraftSessionHandler
import com.velocitypowered.proxy.connection.client.InitialLoginSessionHandler
import com.velocitypowered.proxy.protocol.packet.EncryptionResponsePacket
import moe.caa.multilogin.api.internal.logger.LoggerProvider
import moe.caa.multilogin.api.internal.main.MultiCoreAPI
import moe.caa.multilogin.velocity.injector.handler.MultiInitialLoginSessionHandler
import net.kyori.adventure.text.Component

/**
 * EncryptionResponse 数据包处理
 */
class MultiEncryptionResponse(private val multiCoreAPI: MultiCoreAPI) : EncryptionResponsePacket() {

    override fun handle(handler: MinecraftSessionHandler?): Boolean {
        if (handler !is InitialLoginSessionHandler) {
            return super.handle(handler)
        }
        val multiInitialLoginSessionHandler = MultiInitialLoginSessionHandler(handler, multiCoreAPI)
        try {
            multiInitialLoginSessionHandler.handle(this)
        } catch (e: Throwable) {
            if (multiInitialLoginSessionHandler.encrypted) {
                multiInitialLoginSessionHandler.inbound
                    .disconnect(Component.text(multiCoreAPI.languageHandler.getMessage("auth_error") ?: ""))
            }
            multiInitialLoginSessionHandler.mcConnection.close(true)
            LoggerProvider.logger.error("An exception occurred while processing a login request.", e)
        }
        return true
    }
}
