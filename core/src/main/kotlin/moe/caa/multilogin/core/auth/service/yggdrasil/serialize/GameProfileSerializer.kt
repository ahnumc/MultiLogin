package moe.caa.multilogin.core.auth.service.yggdrasil.serialize

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import moe.caa.multilogin.api.internal.util.ValueUtil.getUuidOrNull
import moe.caa.multilogin.api.profile.GameProfile
import moe.caa.multilogin.api.profile.Property

object GameProfileSerializer : KSerializer<GameProfile> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("GameProfile") {
        element<String>("id")
        element<String>("name", isOptional = true)
        element<List<Property>>("properties", isOptional = true)
    }

    override fun deserialize(decoder: Decoder): GameProfile {
        val json = (decoder as JsonDecoder).decodeJsonElement().jsonObject
        val id = requireNotNull(getUuidOrNull(json["id"]!!.jsonPrimitive.content)) {
            "Invalid UUID in GameProfile"
        }
        val name = json["name"]?.jsonPrimitive?.content ?: ""
        val propertyMap = mutableMapOf<String, Property>()
        json["properties"]?.let { propsEl ->
            // Mojang format: array of {name, value, signature?}
            if (propsEl is kotlinx.serialization.json.JsonArray) {
                for (el in propsEl.jsonArray) {
                    val obj = el.jsonObject
                    val prop = Property(
                        name = obj["name"]?.jsonPrimitive?.content ?: "",
                        value = obj["value"]?.jsonPrimitive?.content ?: "",
                        signature = obj["signature"]?.jsonPrimitive?.content
                    )
                    propertyMap[prop.name] = prop
                }
            }
        }
        return GameProfile(id, name, propertyMap)
    }

    override fun serialize(encoder: Encoder, value: GameProfile) {
        val jsonEncoder = encoder as kotlinx.serialization.json.JsonEncoder
        val obj = kotlinx.serialization.json.buildJsonObject {
            put("id", kotlinx.serialization.json.JsonPrimitive(value.id.toString().replace("-", "")))
            put("name", kotlinx.serialization.json.JsonPrimitive(value.name))
            put("properties", kotlinx.serialization.json.buildJsonArray {
                for ((_, prop) in value.propertyMap) {
                    add(kotlinx.serialization.json.buildJsonObject {
                        put("name", kotlinx.serialization.json.JsonPrimitive(prop.name))
                        put("value", kotlinx.serialization.json.JsonPrimitive(prop.value))
                        prop.signature?.let { put("signature", kotlinx.serialization.json.JsonPrimitive(it)) }
                    })
                }
            })
        }
        jsonEncoder.encodeJsonElement(obj)
    }
}
