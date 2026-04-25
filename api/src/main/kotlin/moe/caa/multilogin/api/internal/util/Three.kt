package moe.caa.multilogin.api.internal.util

import org.jetbrains.annotations.ApiStatus

/**
 * 表示一堆对象
 */
@ApiStatus.Internal
data class Three<V1, V2, V3>(val value1: V1? = null, val value2: V2? = null, val value3: V3? = null)
