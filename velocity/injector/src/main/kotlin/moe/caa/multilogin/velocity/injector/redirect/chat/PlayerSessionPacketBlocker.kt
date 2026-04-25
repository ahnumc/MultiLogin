package moe.caa.multilogin.velocity.injector.redirect.chat

import com.velocitypowered.api.network.ProtocolVersion
import com.velocitypowered.api.proxy.crypto.IdentifiedKey
import com.velocitypowered.proxy.connection.MinecraftSessionHandler
import com.velocitypowered.proxy.protocol.MinecraftPacket
import com.velocitypowered.proxy.protocol.ProtocolUtils
import io.netty.buffer.ByteBuf
import moe.caa.multilogin.api.internal.logger.LoggerProvider
import java.util.*

class PlayerSessionPacketBlocker : MinecraftPacket {
    private var sessionId: UUID? = null
    private var identifiedKey: IdentifiedKey? = null
    private var hasKey = true

    override fun decode(byteBuf: ByteBuf, direction: ProtocolUtils.Direction?, protocolVersion: ProtocolVersion) {
        byteBuf.markReaderIndex()
        try {
            sessionId = ProtocolUtils.readUuid(byteBuf)
            identifiedKey = ProtocolUtils.readPlayerKey(protocolVersion, byteBuf)
        } catch (t: Throwable) {
            byteBuf.resetReaderIndex()
            LoggerProvider.logger.debug("Failed to decode player session packet.", t)
            hasKey = false
        }
    }

    override fun encode(byteBuf: ByteBuf, direction: ProtocolUtils.Direction?, protocolVersion: ProtocolVersion?) {
        //不发送ChatSession
        if (hasKey) {
            ProtocolUtils.writeUuid(byteBuf, sessionId)
            ProtocolUtils.writePlayerKey(byteBuf, identifiedKey)
        }
    }

    override fun handle(minecraftSessionHandler: MinecraftSessionHandler?): Boolean {
        return true
    }
}
