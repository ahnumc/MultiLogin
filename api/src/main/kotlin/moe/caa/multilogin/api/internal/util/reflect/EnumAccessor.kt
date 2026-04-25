package moe.caa.multilogin.api.internal.util.reflect

import org.jetbrains.annotations.ApiStatus

/**
 * 枚举变量访问
 */
@ApiStatus.Internal
class EnumAccessor(val enumClass: Class<*>) {
    @Suppress("UNCHECKED_CAST")
    val values: Array<Enum<*>>
        get() = enumClass.getEnumConstants() as Array<Enum<*>>

    fun indexOf(index: Int): Enum<*> = values[index]

    @Throws(NoSuchEnumException::class)
    fun findByName(name: String?): Enum<*> {
        for (value in values) {
            if (value.name == name) {
                return value
            }
        }
        throw NoSuchEnumException("${enumClass.name} -> $name")
    }
}
