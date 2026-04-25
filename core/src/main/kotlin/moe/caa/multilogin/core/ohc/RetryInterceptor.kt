package moe.caa.multilogin.core.ohc

import moe.caa.multilogin.api.internal.logger.LoggerProvider
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * 延时重试拦截器
 */
class RetryInterceptor(private val retry: Int, private val delay: Long) : Interceptor {
    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        var response: Response
        var tc = 0
        while (true) {
            try {
                response = chain.proceed(request)
                return response
            } catch (e: IOException) {
                LoggerProvider.logger.debug(tc.toString() + " retry failed.", e)
                if (tc >= retry) throw e
            }

            try {
                TimeUnit.MILLISECONDS.sleep(delay)
            } catch (e: InterruptedException) {
                throw InterruptedRetryException(e)
            }
            tc++
            LoggerProvider.logger.debug("--> " + tc + " retry.")
        }
    }
}
