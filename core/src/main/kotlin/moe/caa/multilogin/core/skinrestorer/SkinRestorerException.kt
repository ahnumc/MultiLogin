package moe.caa.multilogin.core.skinrestorer

import java.io.IOException

/**
 * 皮肤修复异常
 */
class SkinRestorerException : IOException {
    constructor()

    constructor(message: String?) : super(message)

    constructor(message: String?, cause: Throwable?) : super(message, cause)

    constructor(cause: Throwable?) : super(cause)
}
