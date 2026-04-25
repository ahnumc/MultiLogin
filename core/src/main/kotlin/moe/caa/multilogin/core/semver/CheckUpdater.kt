package moe.caa.multilogin.core.semver

import com.google.gson.JsonParser
import moe.caa.multilogin.api.internal.logger.LoggerProvider
import moe.caa.multilogin.api.internal.util.ValueUtil
import moe.caa.multilogin.core.main.MultiCore
import moe.caa.multilogin.core.ohc.LoggingInterceptor
import moe.caa.multilogin.core.ohc.RetryInterceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.util.Base64

class CheckUpdater(private val core: MultiCore) {
    @get:Throws(IOException::class)
    val latestVersionNow: List<SemVersion>
        get() {
            val client = OkHttpClient.Builder()
                .addInterceptor(LoggingInterceptor())
                .addInterceptor(RetryInterceptor(3, 10000))
                .writeTimeout(Duration.ofMillis(2000))
                .readTimeout(Duration.ofMillis(2000))
                .connectTimeout(Duration.ofMillis(2000))
                .build()
            val request = Request.Builder().get()
                .url("https://api.github.com/repos/CaaMoe/MultiLogin/contents/latest")
                .build()
            client.newCall(request).execute().use { response ->
                val body = requireNotNull(response.body) { "Latest-version response body is empty." }
                ByteArrayOutputStream().use { baos ->
                    val content = JsonParser.parseString(
                        body.string()
                    ).asJsonObject.getAsJsonPrimitive("content").asString
                    content.split("\n").filter { it.isNotEmpty() }.forEach { s ->
                        baos.writeBytes(Base64.getDecoder().decode(s))
                    }
                    baos.flush()
                    return baos.toString(StandardCharsets.UTF_8)
                        .split("\n")
                        .filter { it.isNotEmpty() }
                        .mapNotNull { SemVersion.of(it) }
                }
            }
    }

    fun start() {
        val server = core.plugin.runServer
        val scheduler = server.scheduler
        scheduler.runTaskAsyncTimer({
            if (!core.pluginConfig.checkUpdate || !core.buildManifest.buildType.equals("final", ignoreCase = true)) return@runTaskAsyncTimer
            try {
                val latestVersionNow = this.latestVersionNow
                if (latestVersionNow.isEmpty()) return@runTaskAsyncTimer
                val currentVersion = core.semVersion
                if (currentVersion == null) {
                    LoggerProvider.logger.info(
                        "The latest version is ${ValueUtil.join(", ", " and ", latestVersionNow.map { it.toString() })}, please update."
                    )
                } else {
                    val sv = latestVersionNow.fold(currentVersion) { acc, version ->
                        if (acc.needUpgrade(version)) version else acc
                    }
                    if (sv != currentVersion) {
                        LoggerProvider.logger.info("The latest recommended version is $sv, Please update.")
                    }
                }
            } catch (e: IOException) {
                LoggerProvider.logger.debug("Check update failure.", e)
            }
        }, 0, 1000 * 60 * 60 * 12)
    }
}
