package moe.caa.multilogin.core.configuration.service

import moe.caa.multilogin.api.service.IService
import moe.caa.multilogin.api.service.ServiceType
import moe.caa.multilogin.core.configuration.ConfException
import moe.caa.multilogin.core.configuration.SkinRestorerConfig

abstract class BaseServiceConfig protected constructor(
    override val serviceId: Int, override val serviceName: String, val initUUID: InitUUID?, val initNameFormat: String,
    val whitelist: Boolean, val skinRestorer: SkinRestorerConfig?
) : IService {
    init {
        checkValid()
    }

    @Throws(ConfException::class)
    protected fun checkValid() {
        if (this.serviceId !in 0..127) throw ConfException(
            "Yggdrasil id ${this.serviceId} is out of bounds, The value can only be between 0 and 127."
        )
    }

    abstract override val serviceType: ServiceType

    enum class InitUUID { DEFAULT }
}
