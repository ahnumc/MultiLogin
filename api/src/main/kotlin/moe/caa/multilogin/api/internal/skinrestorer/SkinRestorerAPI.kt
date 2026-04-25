package moe.caa.multilogin.api.internal.skinrestorer

import moe.caa.multilogin.api.internal.auth.AuthResult
import org.jetbrains.annotations.ApiStatus

@ApiStatus.Internal
interface SkinRestorerAPI {
    /**
     * 进行皮肤修复
     */
    fun doRestorer(result: AuthResult): SkinRestorerResult
}
