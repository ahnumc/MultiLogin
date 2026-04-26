package moe.caa.multilogin.api.internal.handle

import moe.caa.multilogin.api.profile.GameProfile
import org.jetbrains.annotations.ApiStatus

@ApiStatus.Internal
data class OnlineProfileRef(
    val profile: GameProfile,
    val serviceId: Int
)
