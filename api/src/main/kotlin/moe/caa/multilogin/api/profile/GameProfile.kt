package moe.caa.multilogin.api.profile

import java.util.UUID

open class GameProfile(
    open var id: UUID? = null,
    open var name: String? = null,
    open var propertyMap: MutableMap<String, Property> = mutableMapOf()
) : Cloneable {
    public override fun clone(): GameProfile {
        val copiedProperties = propertyMap.entries.associateTo(mutableMapOf()) { (k, v) -> k to v.copy() }
        return GameProfile(id, name, copiedProperties)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GameProfile) return false
        val that = other
        return id == that.id && name == that.name && propertyMap == that.propertyMap
    }

    override fun hashCode(): Int {
        var result = id?.hashCode() ?: 0
        result = 31 * result + (name?.hashCode() ?: 0)
        result = 31 * result + propertyMap.hashCode()
        return result
    }
}
