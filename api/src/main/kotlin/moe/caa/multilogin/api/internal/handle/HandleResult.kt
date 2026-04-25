package moe.caa.multilogin.api.internal.handle

import org.jetbrains.annotations.ApiStatus

/**
 * 表示一个通讯结果
 */
@ApiStatus.Internal
class HandleResult(
    val type: Type,
    val kickMessage: String?
) {
    enum class Type {
        NONE,
        KICK
    }
}
