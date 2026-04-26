package moe.caa.multilogin.api.profile

import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.*

@Serializable
data class GameProfile(
    @Contextual var id: UUID,
    var name: String,
    @SerialName("properties") var propertyMap: MutableMap<String, Property> = mutableMapOf()
)
