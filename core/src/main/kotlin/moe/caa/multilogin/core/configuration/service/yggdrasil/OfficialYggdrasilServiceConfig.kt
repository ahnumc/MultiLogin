package moe.caa.multilogin.core.configuration.service.yggdrasil

import moe.caa.multilogin.api.service.ServiceType
import moe.caa.multilogin.core.configuration.ProxyConfig
import moe.caa.multilogin.core.configuration.SkinRestorerConfig

/**
 * 正版官方 Yggdrasil
 */
class OfficialYggdrasilServiceConfig(
    id: Int,
    name: String?,
    initUUID: InitUUID?,
    initNameFormat: String?,
    whitelist: Boolean,
    skinRestorer: SkinRestorerConfig?,
    trackIp: Boolean,
    timeout: Int,
    retry: Int,
    retryDelay: Long,
    authProxy: ProxyConfig?,
    customSessionServer: String
) : BaseYggdrasilServiceConfig(
    id,
    name,
    initUUID,
    initNameFormat,
    whitelist,
    skinRestorer,
    trackIp,
    timeout,
    retry,
    retryDelay,
    authProxy
) {
    private val customSessionServer: String

    init {
        var customSessionServer = customSessionServer
        if (!customSessionServer.endsWith("/")) {
            customSessionServer = customSessionServer + "/"
        }
        this.customSessionServer = customSessionServer
    }


    override val authURL: String
        get() {
            val baseUrl = customSessionServer
            return baseUrl + "session/minecraft/hasJoined?username={0}&serverId={1}{2}"
        }

    override val authPostContent: String
        get() = throw UnsupportedOperationException("get post content")

    override val authTrackIpContent: String
        get() = "&ip={0}"

    override val httpRequestMethod: HttpRequestMethod
        get() = HttpRequestMethod.GET

    override val serviceType: ServiceType
        get() = ServiceType.OFFICIAL
}
