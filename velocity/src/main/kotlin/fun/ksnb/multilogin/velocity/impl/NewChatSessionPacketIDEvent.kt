package `fun`.ksnb.multilogin.velocity.impl

import com.velocitypowered.api.network.ProtocolVersion
import com.velocitypowered.api.proxy.Player

class NewChatSessionPacketIDEvent(
    val packetID: Int,
    val version: ProtocolVersion,
    val player: Player
)
