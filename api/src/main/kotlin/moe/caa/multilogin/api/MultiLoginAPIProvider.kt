package moe.caa.multilogin.api

import org.jetbrains.annotations.ApiStatus

/**
 * 提供API的地方
 */
@ApiStatus.NonExtendable
object MultiLoginAPIProvider {
    @JvmStatic
    var api: MultiLoginAPI? = null
        private set

    @JvmStatic
    @ApiStatus.Internal
    @Synchronized
    fun setApi(api: MultiLoginAPI) {
        if (MultiLoginAPIProvider.api != null) throw UnsupportedOperationException("duplicate api.")
        MultiLoginAPIProvider.api = api
    }
}
