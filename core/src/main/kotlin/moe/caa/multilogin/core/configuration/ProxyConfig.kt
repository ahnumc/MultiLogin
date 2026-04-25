package moe.caa.multilogin.core.configuration

import moe.caa.multilogin.api.internal.util.ValueUtil.isEmpty
import okhttp3.Authenticator
import okhttp3.Credentials.basic
import okhttp3.Response
import okhttp3.Route
import org.spongepowered.configurate.CommentedConfigurationNode
import org.spongepowered.configurate.serialize.SerializationException
import java.net.InetSocketAddress
import java.net.Proxy

/**
 * 表示一个代理配置
 */
class ProxyConfig(
    private val type: Proxy.Type?,
    private val hostname: String?,
    private val port: Int,
    private val username: String?,
    private val password: String?
) {
    val proxy: Proxy
        get() {
            val proxyType = requireNotNull(type)
            if (proxyType == Proxy.Type.DIRECT) return Proxy.NO_PROXY
            return Proxy(proxyType, InetSocketAddress(requireNotNull(hostname), port))
        }

    val proxyAuthenticator: Authenticator
        get() = Authenticator { _: Route?, response: Response? ->
            val authUsername = username?.takeUnless { isEmpty(it) } ?: return@Authenticator null
            val credential = basic(authUsername, requireNotNull(password))
            requireNotNull(response).request.newBuilder()
                .header("Proxy-Authorization", credential)
                .build()
        }

    companion object {
        @Throws(SerializationException::class, ConfException::class)
        fun read(node: CommentedConfigurationNode): ProxyConfig {
            val type = node.node("type").get(Proxy.Type::class.java, Proxy.Type.DIRECT)
            val hostname = node.node("hostname").getString("127.0.0.1")
            val port = node.node("port").getInt(1080)
            val username = node.node("username").getString("")
            val password = node.node("password").getString("")

            return ProxyConfig(type, hostname, port, username, password)
        }
    }
}
