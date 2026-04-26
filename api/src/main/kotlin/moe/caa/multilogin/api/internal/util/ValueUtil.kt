package moe.caa.multilogin.api.internal.util

import org.jetbrains.annotations.ApiStatus
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.*

@ApiStatus.Internal
object ValueUtil {
    @JvmStatic
    fun uuidToBytes(uuid: UUID): ByteArray {
        val uuidBytes = ByteArray(16)
        ByteBuffer.wrap(uuidBytes).order(ByteOrder.BIG_ENDIAN)
            .putLong(uuid.mostSignificantBits).putLong(uuid.leastSignificantBits)
        return uuidBytes
    }

    @JvmStatic
    fun bytesToUuid(bytes: ByteArray): UUID? {
        if (bytes.size != 16) return null
        val buf = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN)
        return UUID(buf.long, buf.long)
    }

    @JvmStatic
    fun getUuidOrNull(uuid: String): UUID? = runCatching {
        UUID.fromString(uuid.replaceFirst("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})".toRegex(), "$1-$2-$3-$4-$5"))
    }.getOrNull()

    @JvmStatic
    fun isEmpty(str: String?): Boolean = str.isNullOrEmpty()

    @JvmStatic
    fun transPapi(s: String, vararg pairs: Pair<String, Any?>): String =
        pairs.foldIndexed(s) { i, acc, (key, value) ->
            acc.replace("{$key}", value.toString()).replace("{$i}", value.toString())
        }

    @JvmStatic
    fun transPapi(s: String, pairs: List<Pair<String, Any?>>): String =
        pairs.foldIndexed(s) { i, acc, (key, value) ->
            acc.replace("{$key}", value.toString()).replace("{$i}", value.toString())
        }

    fun join(delimiter: CharSequence, lastDelimiter: CharSequence?, vararg elements: Any?): String {
        if (elements.isEmpty()) return ""
        if (elements.size == 1) return elements[0].toString()
        return elements.dropLast(1).joinToString(delimiter) + lastDelimiter + elements.last()
    }

    fun join(delimiter: CharSequence, lastDelimiter: CharSequence?, elements: MutableCollection<out Any?>): String =
        join(delimiter, lastDelimiter, *elements.toTypedArray<Any?>())

    @JvmStatic
    @Throws(NoSuchAlgorithmException::class)
    fun sha256(str: String): ByteArray =
        MessageDigest.getInstance("SHA-256").digest(str.toByteArray(StandardCharsets.UTF_8))

    @JvmStatic
    fun xuidToUUID(xuid: String): UUID = UUID(0, xuid.toLong())

    @JvmStatic
    fun generateLinkCode(): String = (0..5).joinToString("") { (10 * Math.random()).toInt().toString() }
}
