package moe.caa.multilogin.core.ohc

import moe.caa.multilogin.api.internal.logger.LoggerProvider
import okhttp3.Interceptor
import okhttp3.Response
import okio.Buffer
import java.io.IOException
import java.util.concurrent.TimeUnit

class LoggingInterceptor : Interceptor {
    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        LoggerProvider.logger.debug("--> ${request.method} ${request.url}")

        request.body?.let { body ->
            val bf = Buffer()
            body.writeTo(bf)
            val size = bf.size
            if (size > 0) LoggerProvider.logger.debug("--> ($size bytes)")
        }

        val startNs = System.nanoTime()
        val response = try {
            chain.proceed(request)
        } catch (e: Exception) {
            LoggerProvider.logger.debug("<-- HTTP FAILED", e)
            throw e
        }
        val tookMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNs)

        LoggerProvider.logger.debug("<-- ${response.code} ${response.request.url} (${tookMs}ms)")
        response.body?.let { body ->
            val source = body.source()
            source.request(Long.MAX_VALUE)
            val size = source.buffer.size
            if (size > 0) LoggerProvider.logger.debug("<-- ($size bytes)")
        }

        return response
    }
}
