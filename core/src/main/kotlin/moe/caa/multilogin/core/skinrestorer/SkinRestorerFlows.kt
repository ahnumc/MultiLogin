package moe.caa.multilogin.core.skinrestorer

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import moe.caa.multilogin.api.internal.logger.LoggerProvider
import moe.caa.multilogin.api.internal.util.ValueUtil.sha256
import moe.caa.multilogin.api.profile.GameProfile
import moe.caa.multilogin.api.profile.Property
import moe.caa.multilogin.core.configuration.SkinRestorerConfig
import moe.caa.multilogin.core.configuration.service.BaseServiceConfig
import moe.caa.multilogin.core.main.MultiCore
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayInputStream
import java.io.IOException
import java.util.*
import java.util.concurrent.Callable
import javax.imageio.ImageIO

/**
 * 皮肤修复工作流
 */
class SkinRestorerFlows(
    private val core: MultiCore,
    private val config: BaseServiceConfig,
    private val okHttpClient: OkHttpClient,
    private val skinUrl: String,
    private val skinModel: String,
    private val profile: GameProfile
) : Callable<SkinRestorerResultImpl?> {
    @Throws(Exception::class)
    override fun call(): SkinRestorerResultImpl {
        val restorerConfig = requireNotNull(config.skinRestorer)
        val bytes: ByteArray
        try {
            bytes = requireValidSkin(skinUrl, skinModel)
        } catch (e: Exception) {
            return SkinRestorerResultImpl.ofBadSkin(e)
        }

        val request: Request = if (restorerConfig.method == SkinRestorerConfig.Method.UPLOAD) {
            Request.Builder()
                .url("https://api.mineskin.org/generate/upload")
                .header("User-Agent", "MultiLogin/v2.0")
                .post(
                    MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("name", UUID.randomUUID().toString().substring(0, 6))
                        .addFormDataPart("variant", skinModel)
                        .addFormDataPart("visibility", "0")
                        .addFormDataPart(
                            "file", "upload.png",
                            bytes.toRequestBody("multipart/form-data".toMediaTypeOrNull())
                        )
                        .build()
                )
                .build()
        } else {
            val jo = JsonObject()
            jo.addProperty("name", UUID.randomUUID().toString().substring(0, 6))
            jo.addProperty("variant", skinModel)
            jo.addProperty("visibility", 0)
            jo.addProperty("url", skinUrl)

            Request.Builder()
                .url("https://api.mineskin.org/generate/url")
                .header("User-Agent", core.httpRequestHeaderUserAgent)
                .header("Content-Type", "application/json")
                .post(jo.toString().toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull()))
                .build()
        }

        val jo = okHttpClient.newCall(request).execute().use { response ->
            val body = requireNotNull(response.body) { "Mineskin response body is empty" }
            JsonParser.parseString(body.string()).asJsonObject
                .getAsJsonObject("data")
                .getAsJsonObject("texture")
        }
        val value = jo.getAsJsonPrimitive("value").asString
        val signature = jo.getAsJsonPrimitive("signature").asString
        try {
            requireNotNull(core.sqlManager.skinRestoredCacheTable).insertNew(sha256(skinUrl), skinModel, value, signature)
        } catch (e: Exception) {
            LoggerProvider.logger.warn("An exception occurred while saving restored skin data.", e)
        }
        val restoredProperty = Property()
        restoredProperty.name = "textures"
        restoredProperty.value = value
        restoredProperty.signature = signature
        profile.propertyMap["textures"] = restoredProperty
        return SkinRestorerResultImpl.ofRestorerSucceed(profile)
    }

    @Throws(IOException::class)
    private fun requireValidSkin(skinUrl: String, model: String?): ByteArray {
        val request = Request.Builder()
            .get()
            .header("User-Agent", "MultiLogin/v2.0")
            .url(skinUrl)
            .build()
        val bytes = okHttpClient.newCall(request).execute().use { response ->
            requireNotNull(response.body) { "Skin response body is empty" }.bytes()
        }
        ByteArrayInputStream(bytes).use { bais ->
            val image = ImageIO.read(bais)
            if (image.width != 64) {
                throw SkinRestorerException("Skin width is not 64.")
            }
            if (!(image.height == 32 || image.height == 64)) {
                throw SkinRestorerException("Skin height is not 64 or 32.")
            }
            return bytes
        }
    }
}
