package moe.caa.multilogin.core.configuration.service

import moe.caa.multilogin.api.service.IService
import moe.caa.multilogin.api.service.ServiceType
import moe.caa.multilogin.core.configuration.ConfException
import moe.caa.multilogin.core.configuration.SkinRestorerConfig
import java.nio.charset.StandardCharsets
import java.util.*

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

    fun generateName(loginName: String): String {
        return initNameFormat.replace("{name}", loginName).replace(" ", "_")
    }

    abstract override val serviceType: ServiceType

    enum class InitUUID(private val generator: (UUID?, String?) -> UUID?) {
        DEFAULT({ u, _ -> u }),
        OFFLINE({ _, n -> UUID.nameUUIDFromBytes("OfflinePlayer:$n".toByteArray(StandardCharsets.UTF_8)) }),
        RANDOM({ _, _ -> UUID.randomUUID() });

        fun generateUUID(onlineUUID: UUID?, currentUsername: String?): UUID? = generator(onlineUUID, currentUsername)
    }
}
