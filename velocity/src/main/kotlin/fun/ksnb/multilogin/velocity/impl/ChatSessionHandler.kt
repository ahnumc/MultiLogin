package `fun`.ksnb.multilogin.velocity.impl

import com.velocitypowered.api.event.EventManager
import com.velocitypowered.api.proxy.Player
import com.velocitypowered.proxy.protocol.ProtocolUtils
import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelDuplexHandler
import io.netty.channel.ChannelHandlerContext

class ChatSessionHandler(private val player: Player, private val eventManager: EventManager) : ChannelDuplexHandler() {
    @Throws(Exception::class)
    override fun channelRead(
        ctx: ChannelHandlerContext,
        packet: Any
    ) {
        if (packet is ByteBuf) {
            val c = packet.asReadOnly()
            c.markReaderIndex()
            try {
                val packetId = c.readByte().toInt()
                ProtocolUtils.readUuid(c)
                ProtocolUtils.readPlayerKey(player.getProtocolVersion(), c)
                eventManager.fire(
                    NewChatSessionPacketIDEvent(
                        packetId,
                        player.protocolVersion,
                        player
                    )
                )
            } catch (_: Throwable) {
            } finally {
                c.resetReaderIndex()
            }
        }
        super.channelRead(ctx, packet)
    }
}
