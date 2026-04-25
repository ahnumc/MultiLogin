package moe.caa.multilogin.api.internal.skinrestorer

import moe.caa.multilogin.api.profile.GameProfile
import org.jetbrains.annotations.ApiStatus

@ApiStatus.Internal
interface SkinRestorerResult {
    val reason: Reason
    val response: GameProfile?
    val throwable: Throwable?

    enum class Reason {
        NO_SKIN,
        NO_RESTORER,
        USE_CACHE,
        SIGNATURE_VALID,
        BAD_SKIN,
        RESTORER_SUCCEED,
        RESTORER_ASYNC,
        RESTORER_FAILED
    }
}
