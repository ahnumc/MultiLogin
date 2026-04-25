package moe.caa.multilogin.core.auth.service.yggdrasil

import moe.caa.multilogin.api.profile.GameProfile
import moe.caa.multilogin.api.profile.Property
import java.util.UUID

class UnmodifiableGameProfile(id: UUID?, name: String?, propertyMap: MutableMap<String, Property>) :
    GameProfile(id, name, propertyMap) {
    override var id: UUID?
        get() = super.id
        set(_) { throw UnsupportedOperationException() }

    override var name: String?
        get() = super.name
        set(_) { throw UnsupportedOperationException() }

    override var propertyMap: MutableMap<String, Property>
        get() = super.propertyMap.entries.associateTo(LinkedHashMap()) { (k, v) -> k to v.copy() }
        set(_) { throw UnsupportedOperationException() }

    companion object {
        fun unmodifiable(profile: GameProfile): UnmodifiableGameProfile =
            UnmodifiableGameProfile(profile.id, profile.name, profile.propertyMap)
    }
}
