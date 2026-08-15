package moe.caa.multilogin.velocity.injector.redirect.chat

import com.velocitypowered.api.network.ProtocolVersion
import com.velocitypowered.proxy.connection.MinecraftSessionHandler
import com.velocitypowered.proxy.protocol.MinecraftPacket
import com.velocitypowered.proxy.protocol.ProtocolUtils
import io.netty.buffer.ByteBuf

class PlayerSessionPacketBlocker : MinecraftPacket {
    override fun decode(buf: ByteBuf, direction: ProtocolUtils.Direction, version: ProtocolVersion) {
        buf.skipBytes(buf.readableBytes())
    }

    override fun encode(buf: ByteBuf, direction: ProtocolUtils.Direction, version: ProtocolVersion) = Unit

    override fun handle(handler: MinecraftSessionHandler): Boolean = true
}
