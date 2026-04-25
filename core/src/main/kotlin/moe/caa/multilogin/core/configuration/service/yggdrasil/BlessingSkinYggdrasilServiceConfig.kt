package moe.caa.multilogin.core.configuration.service.yggdrasil

import moe.caa.multilogin.api.service.ServiceType
import moe.caa.multilogin.core.configuration.ProxyConfig
import moe.caa.multilogin.core.configuration.SkinRestorerConfig

/**
 * Blessing Skin 皮肤站 Yggdrasil
 */
class BlessingSkinYggdrasilServiceConfig(
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
    apiRoot: String
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
    private val apiRoot: String

    init {
        var apiRoot = apiRoot
        if (!apiRoot.endsWith("/")) {
            apiRoot = apiRoot + "/"
        }
        this.apiRoot = apiRoot
    }


    override val authURL: String
        get() = apiRoot + "session" + "server" + "/session" + "/minecraft" + "/hasJoined?" + "username={0}&serverId={1}{2}"

    override val authPostContent: String
        get() = throw UnsupportedOperationException()

    override val authTrackIpContent: String
        get() = "&ip={0}"

    override val httpRequestMethod: HttpRequestMethod
        get() = HttpRequestMethod.GET

    override val serviceType: ServiceType
        get() = ServiceType.BLESSING_SKIN
}
