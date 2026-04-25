package moe.caa.multilogin.core.configuration

import org.spongepowered.configurate.CommentedConfigurationNode
import org.spongepowered.configurate.serialize.SerializationException

/**
 * 表示一个皮肤修复配置
 */
class SkinRestorerConfig private constructor(
    val restorer: RestorerType?,
    val method: Method?,
    val timeout: Int,
    val retry: Int,
    val retryDelay: Int,
    val proxy: ProxyConfig?
) {
    enum class RestorerType {
        OFF, LOGIN, ASYNC
    }

    enum class Method {
        URL, UPLOAD
    }

    companion object {
        @Throws(SerializationException::class, ConfException::class)
        fun read(node: CommentedConfigurationNode): SkinRestorerConfig {
            val restorer = node.node("restorer").get(RestorerType::class.java, RestorerType.OFF)
            val method = node.node("method").get(Method::class.java, Method.URL)
            val timeout = node.node("timeout").getInt(10000)
            val retry = node.node("retry").getInt(2)
            val retryDelay = node.node("retryDelay").getInt(5000)
            val proxy: ProxyConfig = ProxyConfig.read(node.node("proxy"))

            return SkinRestorerConfig(restorer, method, timeout, retry, retryDelay, proxy)
        }
    }
}
