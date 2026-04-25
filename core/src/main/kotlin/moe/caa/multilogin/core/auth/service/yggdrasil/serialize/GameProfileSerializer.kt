package moe.caa.multilogin.core.auth.service.yggdrasil.serialize

import com.google.gson.*
import moe.caa.multilogin.api.internal.util.ValueUtil.getUuidOrNull
import moe.caa.multilogin.api.profile.GameProfile
import moe.caa.multilogin.api.profile.Property
import moe.caa.multilogin.core.auth.service.yggdrasil.UnmodifiableGameProfile
import java.lang.reflect.Type

/**
 * GameProfile 的 GSON 序列化程序
 */
class GameProfileSerializer : JsonSerializer<GameProfile?>, JsonDeserializer<GameProfile?> {
    @Throws(JsonParseException::class)
    override fun deserialize(json: JsonElement, typeOfT: Type?, context: JsonDeserializationContext): GameProfile {
        val ret = GameProfile()
        if (json.isJsonObject) {
            val root = json.asJsonObject
            ret.id = getUuidOrNull(root.get("id").asString)
            if (root.has("name")) ret.name = root.get("name").asString
            val propertiesJsonElement = root.get("properties")
            propertiesJsonElement?.let { props ->
                if (props.isJsonObject) {
                    for (entry in props.asJsonObject.entrySet()) {
                        if (entry.value.isJsonArray) {
                            for (ignored in entry.value.asJsonArray) {
                                ret.propertyMap[entry.key] = context.deserialize(ignored, Property::class.java)
                            }
                        }
                    }
                } else if (props.isJsonArray) {
                    for (element in props.asJsonArray) {
                        val value = context.deserialize<Property>(element, Property::class.java)
                        value.name?.let { ret.propertyMap[it] = value }
                    }
                }
            }
        }
        return UnmodifiableGameProfile.unmodifiable(ret)
    }

    override fun serialize(src: GameProfile?, typeOfSrc: Type?, context: JsonSerializationContext): JsonElement {
        val ret = JsonObject()
        src?.id?.toString()?.replace("-", "")?.let { ret.addProperty("id", it) }
        src?.name?.let { ret.addProperty("name", it) }
        val propertiesJsonArray = JsonArray()
        ret.add("properties", propertiesJsonArray)
        for (entry in src?.propertyMap.orEmpty()) {
            propertiesJsonArray.add(context.serialize(entry.value, Property::class.java))
        }
        return ret
    }
}
