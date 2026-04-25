package moe.caa.multilogin.core.auth.service.yggdrasil.serialize

import com.google.gson.*
import moe.caa.multilogin.api.profile.Property
import java.lang.reflect.Type

/**
 * Property 的 GSON 序列化程序
 */
class PropertySerializer : JsonSerializer<Property?>, JsonDeserializer<Property?> {
    @Throws(JsonParseException::class)
    override fun deserialize(json: JsonElement, typeOfT: Type?, context: JsonDeserializationContext?): Property {
        val ret = Property()
        if (json.isJsonObject) {
            val root = json.asJsonObject
            ret.name = root.get("name").asString
            ret.value = root.get("value").asString
            if (root.has("signature")) ret.signature = root.get("signature").asString
        }
        return ret
    }

    override fun serialize(src: Property?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement {
        val ret = JsonObject()
        ret.addProperty("name", src?.name)
        ret.addProperty("value", src?.value)
        src?.signature?.let { ret.addProperty("signature", it) }
        return ret
    }
}
