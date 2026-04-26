package `fun`.ksnb.multilogin.velocity.main

import com.velocitypowered.api.event.PostOrder
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.connection.DisconnectEvent
import com.velocitypowered.api.event.connection.LoginEvent
import `fun`.ksnb.multilogin.velocity.impl.VelocityPlayer
import moe.caa.multilogin.api.internal.handle.HandleResult
import net.kyori.adventure.text.Component

/**
 * Velocity 的事件处理程序
 */
class GlobalListener(private val multiLoginVelocity: MultiLoginVelocity) {
    private val coreApi
        get() = requireNotNull(multiLoginVelocity.multiCoreAPI)

    fun register() {
        multiLoginVelocity.server.eventManager.register(multiLoginVelocity, this)
    }

    @Subscribe(order = PostOrder.FIRST)
    fun onPlayerJoin(event: LoginEvent) {
        val result: HandleResult = coreApi.playerHandler.pushPlayerJoinGame(
            event.player.uniqueId,
            event.player.username
        )
        if (result.type === HandleResult.Type.KICK) {
            val msg = result.kickMessage
            if (msg.isNullOrBlank()) {
                event.player.disconnect(Component.text(""))
            } else {
                event.player.disconnect(Component.text(msg))
            }
            return
        }

        coreApi.playerHandler.callPlayerJoinGame(VelocityPlayer(event.player))
    }

    @Subscribe(order = PostOrder.FIRST)
    fun onDisconnect(event: DisconnectEvent) {
        coreApi.playerHandler.pushPlayerQuitGame(
            event.player.uniqueId,
            event.player.username
        )
    }
}
