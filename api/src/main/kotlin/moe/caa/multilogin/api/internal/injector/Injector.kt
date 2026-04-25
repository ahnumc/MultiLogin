package moe.caa.multilogin.api.internal.injector

import moe.caa.multilogin.api.internal.main.MultiCoreAPI
import org.jetbrains.annotations.ApiStatus

/**
 * 子模块注入接口
 */
@ApiStatus.Internal
interface Injector {
    /**
     * 开始注入
     */
    @Throws(Throwable::class)
    fun inject(api: MultiCoreAPI)
    fun registerChatSession(packetMapping: MutableMap<Int, Int>)
}
