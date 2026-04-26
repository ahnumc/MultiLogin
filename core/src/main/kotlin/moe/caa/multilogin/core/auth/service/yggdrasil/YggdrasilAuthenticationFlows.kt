package moe.caa.multilogin.core.auth.service.yggdrasil

import moe.caa.multilogin.api.profile.GameProfile
import moe.caa.multilogin.core.auth.service.yggdrasil.serialize.GameProfileSerializer
import moe.caa.multilogin.core.configuration.service.yggdrasil.BaseYggdrasilServiceConfig
import moe.caa.multilogin.core.configuration.service.yggdrasil.BaseYggdrasilServiceConfig.HttpRequestMethod
import moe.caa.multilogin.core.main.MultiCore
import moe.caa.multilogin.core.ohc.LoggingInterceptor
import moe.caa.multilogin.core.ohc.RetryInterceptor
import moe.caa.multilogin.flows.workflows.BaseFlows
import moe.caa.multilogin.flows.workflows.Signal
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.time.Duration

/**
 * 一个工作流，进行对 Yggd 的 hasJoined 访问
 */
class YggdrasilAuthenticationFlows(
    private val core: MultiCore,
    private val username: String,
    private val serverId: String,
    private val ip: String?,
    private val config: BaseYggdrasilServiceConfig
) : BaseFlows<HasJoinedContext?>() {
    @Throws(Exception::class)
    fun call(): GameProfile? {
        val url = config.generateAuthURL(username, serverId, ip)

        val request = when (config.httpRequestMethod) {
            HttpRequestMethod.GET -> Request.Builder()
                .get().url(url)
                .header("User-Agent", core.httpRequestHeaderUserAgent)
                .build()
            HttpRequestMethod.POST -> Request.Builder()
                .post("".toRequestBody()).url(url)
                .header("User-Agent", core.httpRequestHeaderUserAgent)
                .header("Content-Type", "application/json")
                .build()
            else -> throw UnsupportedOperationException("HttpRequestMethod")
        }
        return call0(config, request)
    }

    @Throws(IOException::class)
    private fun call0(config: BaseYggdrasilServiceConfig, request: Request): GameProfile? {
        val client = OkHttpClient.Builder()
            .addInterceptor(RetryInterceptor(config.retry, config.retryDelay))
            .addInterceptor(LoggingInterceptor())
            .writeTimeout(Duration.ofMillis(config.timeout.toLong()))
            .readTimeout(Duration.ofMillis(config.timeout.toLong()))
            .connectTimeout(Duration.ofMillis(config.timeout.toLong()))
            .proxy(config.authProxy?.proxy)
            .proxyAuthenticator(config.authProxy?.proxyAuthenticator ?: okhttp3.Authenticator.NONE)
            .build()
        val call = client.newCall(request)
        call.execute().use { execute ->
            val body = execute.body ?: return null
            val text = body.string()
            if (text.isBlank()) return null
            return core.gson.decodeFromString(GameProfileSerializer, text)
        }
    }

    override fun run(context: HasJoinedContext?): Signal {
        val hasJoinedContext = requireNotNull(context)
        try {
            val call = call()?.takeIf { it.id != null } ?: return Signal.TERMINATED
            hasJoinedContext.response.set(call to config)
            return Signal.PASSED
        } catch (e: Throwable) {
            hasJoinedContext.serviceUnavailable[config] = e
            return Signal.TERMINATED
        }
    }
}
