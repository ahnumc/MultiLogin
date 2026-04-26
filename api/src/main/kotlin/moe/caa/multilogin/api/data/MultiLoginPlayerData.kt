package moe.caa.multilogin.api.data

import moe.caa.multilogin.api.profile.GameProfile
import moe.caa.multilogin.api.service.IService
import org.jetbrains.annotations.ApiStatus

/**
 * 表示一个使用猫踢螺钉登录的玩家的登录数据
 */
@ApiStatus.NonExtendable
interface MultiLoginPlayerData {
    fun getOnlineProfile(): GameProfile
    val loginService: IService
}
