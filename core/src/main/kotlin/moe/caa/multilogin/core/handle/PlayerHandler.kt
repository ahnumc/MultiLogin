package moe.caa.multilogin.core.handle

import moe.caa.multilogin.api.data.MultiLoginPlayerData
import moe.caa.multilogin.api.internal.handle.HandleResult
import moe.caa.multilogin.api.internal.handle.HandlerAPI
import moe.caa.multilogin.api.internal.handle.OnlineProfileRef
import moe.caa.multilogin.api.internal.logger.LoggerProvider
import moe.caa.multilogin.api.internal.plugin.IPlayer
import moe.caa.multilogin.api.profile.GameProfile
import moe.caa.multilogin.api.service.IService
import moe.caa.multilogin.core.configuration.service.BaseServiceConfig
import moe.caa.multilogin.core.main.MultiCore
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * 数据缓存中心
 */
class PlayerHandler(private val core: MultiCore) : HandlerAPI {
    // inGameUUID \ Entry
    private val cache = ConcurrentHashMap<UUID, Entry>()

    // inGameUUID \ Entry
    val loginCache = ConcurrentHashMap<UUID, Entry>()

    override fun pushPlayerQuitGame(inGameUUID: UUID?, username: String?): HandleResult =
        HandleResult(HandleResult.Type.NONE, null)

    override fun pushPlayerJoinGame(inGameUUID: UUID?, username: String?): HandleResult {
        val remove = loginCache.remove(inGameUUID ?: return HandleResult(HandleResult.Type.NONE, null))
        if (remove == null) {
            if (core.pluginConfig.forceUseLogin) {
                return HandleResult(
                    HandleResult.Type.KICK,
                    core.languageHandler.getMessage("auth_handler_need_use_login")
                )
            }
            LoggerProvider.logger.warn(
                "The player with in game UUID $inGameUUID and name $username is not logged into the server by MultiLogin, some features will be disabled for him."
            )
        } else {
            val l = System.currentTimeMillis() - remove.signTimeMillis
            if (l > 5 * 1000) {
                LoggerProvider.logger.warn(
                    "Players with in game UUID $inGameUUID and name $username are taking too long to log in after verification, reached $l milliseconds. Is it the same person?"
                )
            }
            cache[inGameUUID] = remove
        }

        return HandleResult(HandleResult.Type.NONE, null)
    }

    override fun callPlayerJoinGame(player: IPlayer) {
        if (!core.pluginConfig.welcomeMsg) return

        core.plugin.runServer.scheduler.runTaskAsync({
            val message = getPlayerOnlineProfile0(player.uniqueId)?.let { (onlineProfile, serviceConfig) ->
                core.languageHandler.getMessage(
                    "welcome_msg",
                    "online_name" to onlineProfile.name,
                    "online_uuid" to onlineProfile.id,
                    "service_name" to serviceConfig.serviceName,
                    "service_id" to serviceConfig.serviceId,
                    "profile_name" to player.name,
                    "profile_uuid" to player.uniqueId
                )
            } ?: core.languageHandler.getMessage(
                "welcome_msg_to_unknown",
                "profile_name" to player.name,
                "profile_uuid" to player.name
            )
            player.sendMessagePL(message)
        }, 3000)
    }

    fun getPlayerData(inGameUUID: UUID?): MultiLoginPlayerData? = cache[inGameUUID]

    override fun getPlayerOnlineProfile(inGameUUID: UUID?): OnlineProfileRef? {
        val entry = cache[inGameUUID] ?: return null
        return OnlineProfileRef(entry.onlineProfileData, entry.serviceConfig.serviceId)
    }

    fun getPlayerOnlineProfile0(inGameUUID: UUID?): Pair<GameProfile, BaseServiceConfig>? {
        val entry = cache[inGameUUID] ?: return null
        return entry.onlineProfileData to entry.serviceConfig
    }

    override fun getInGameUUID(onlineUUID: UUID?, serviceId: Int): UUID? =
        cache.entries.find { (_, v) ->
            v.onlineProfileData.id == onlineUUID && v.serviceConfig.serviceId == serviceId
        }?.key

    override fun getServiceName(serviceId: Int): String? =
        core.pluginConfig.serviceIdMap[serviceId]?.serviceName

    fun register() {
        core.plugin.runServer.scheduler.runTaskAsyncTimer({
            val onlinePlayerUUIDs = core.plugin.runServer.playerManager.onlinePlayers
                .map(IPlayer::uniqueId)
                .toSet()

            val noExists = cache.entries.filter { (key, _) -> key !in onlinePlayerUUIDs }.toSet()

            try {
                Thread.sleep(10000)
            } catch (e: InterruptedException) {
                LoggerProvider.logger.error("An exception occurred on the delayed cache clearing.", e)
            }

            for ((key, value) in noExists) {
                if (cache[key] != value) continue
                cache.remove(key)
            }
        }, 0, 1000 * 60)
    }

    data class Entry(
        val onlineProfileData: GameProfile,
        val serviceConfig: BaseServiceConfig,
        val signTimeMillis: Long
    ) : MultiLoginPlayerData {
        override fun getOnlineProfile(): GameProfile = onlineProfileData

        override val loginService: IService
            get() = serviceConfig
    }
}
