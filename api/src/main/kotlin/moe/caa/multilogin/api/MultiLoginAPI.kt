package moe.caa.multilogin.api

import moe.caa.multilogin.api.data.MultiLoginPlayerData
import moe.caa.multilogin.api.service.IService
import org.jetbrains.annotations.ApiStatus
import java.util.*

/**
 * 猫踢螺钉的API, 全部都在这里了
 */
@ApiStatus.NonExtendable
interface MultiLoginAPI {
    /**
     * 返回所有验证服务列表
     * @return 所有验证服务列表
     */
    val services: MutableCollection<out IService>

    /**
     * 通过游戏内 uuid, 返回玩家的登录数据
     * @param inGameUUID 游戏内uuid
     * @return 玩家的登录数据
     */
    fun getPlayerData(inGameUUID: UUID): MultiLoginPlayerData?
}
