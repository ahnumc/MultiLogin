package moe.caa.multilogin.api.profile

import kotlinx.serialization.Serializable

@Serializable
data class Property(
    val name: String = "",
    val value: String = "",
    val signature: String? = null
)
