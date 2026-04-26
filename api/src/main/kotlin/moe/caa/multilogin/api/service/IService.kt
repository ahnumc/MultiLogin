package moe.caa.multilogin.api.service

import org.jetbrains.annotations.ApiStatus

/**
 * 表示一个验证服务器
 */
@ApiStatus.NonExtendable
interface IService {
    /**
     * 返回这个验证服务ID
     * @return 这个验证服务ID
     */
    val serviceId: Int

    /**
     * 返回验证服务名字
     * @return 验证服务名字
     */
    val serviceName: String

    /**
     * 返回验证服务类型
     * @return 验证服务类型
     */
    val serviceType: ServiceType
}
