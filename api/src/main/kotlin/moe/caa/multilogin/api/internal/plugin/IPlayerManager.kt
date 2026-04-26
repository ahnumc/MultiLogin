package moe.caa.multilogin.api.internal.plugin

import org.jetbrains.annotations.ApiStatus
import java.util.*

/**
 * 公共玩家管理器对象
 */
@ApiStatus.Internal
interface IPlayerManager {
    fun getPlayers(name: String): MutableSet<IPlayer>
    fun getPlayer(uuid: UUID): IPlayer?
    val onlinePlayers: MutableSet<IPlayer>

    fun kickPlayerIfOnline(name: String, message: String?) {
        for (player in getPlayers(name)) {
            player.kickPlayer(message)
        }
    }

    fun kickAll(message: String?) {
        for (player in onlinePlayers) {
            player.kickPlayer(message)
        }
    }

    fun kickPlayerIfOnline(uuid: UUID, message: String?) {
        getPlayer(uuid)?.kickPlayer(message)
    }

    fun hasOnline(redirectUuid: UUID): Boolean {
        return getPlayer(redirectUuid) != null
    }
}
