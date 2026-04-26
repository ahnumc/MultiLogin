package moe.caa.multilogin.velocity.injector.redirect.auth

import com.velocitypowered.proxy.connection.MinecraftSessionHandler
import com.velocitypowered.proxy.protocol.packet.ServerLoginPacket
import moe.caa.multilogin.api.internal.main.MultiCoreAPI

/**
 * 擦除登录验证签名的包
 */
class MultiServerLogin(private val multiCoreAPI: MultiCoreAPI?) : ServerLoginPacket() {

    override fun handle(handler: MinecraftSessionHandler?): Boolean {
        return super.handle(handler)
    }
}
