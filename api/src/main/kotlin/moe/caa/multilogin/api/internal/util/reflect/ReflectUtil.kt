package moe.caa.multilogin.api.internal.util.reflect

import org.jetbrains.annotations.ApiStatus
import java.lang.reflect.*

@ApiStatus.Internal
object ReflectUtil {
    @JvmStatic
    fun handleAccessible(method: Method): Method = method.also { it.isAccessible = true }

    fun <T> handleAccessible(constructor: Constructor<T?>): Constructor<T?> = constructor.also { it.isAccessible = true }

    @JvmStatic
    fun handleAccessible(field: Field): Field = field.also { it.isAccessible = true }

    @Throws(NoSuchFieldException::class)
    fun findNoStaticField(target: Class<*>, fieldType: Type): Field {
        val all = target.declaredFields.toList() + target.fields.toList()
        return all.firstOrNull { !Modifier.isStatic(it.modifiers) && it.type == fieldType }
            ?: throw NoSuchFieldException("Type: ${fieldType.typeName}")
    }

    @Throws(NoSuchMethodException::class)
    fun findNoStaticMethodByParameters(target: Class<*>, vararg fieldTypes: Type?): Method =
        target.declaredMethods.firstOrNull { !Modifier.isStatic(it.modifiers) && it.parameterTypes.contentEquals(fieldTypes) }
            ?: throw NoSuchMethodException("${target.name} Types: ${fieldTypes.contentToString()}")

    @Throws(NoSuchMethodException::class)
    fun findStaticMethodByParameters(target: Class<*>, vararg fieldTypes: Type?): Method =
        target.declaredMethods.firstOrNull { Modifier.isStatic(it.modifiers) && it.parameterTypes.contentEquals(fieldTypes) }
            ?: throw NoSuchMethodException("${target.name} Types: ${fieldTypes.contentToString()}")

    @Throws(NoSuchMethodException::class)
    fun findStaticMethodByReturnTypeAndParameters(target: Class<*>, returnType: Type, vararg fieldTypes: Type?): Method =
        target.declaredMethods.firstOrNull {
            Modifier.isStatic(it.modifiers) && it.parameterTypes.contentEquals(fieldTypes) && returnType == it.returnType
        } ?: throw NoSuchMethodException("${target.name} Types: ${fieldTypes.contentToString()}")

    @Throws(NoSuchMethodException::class)
    fun findNoStaticMethodByReturnType(target: Class<*>, returnType: Type?): Method =
        target.declaredMethods.firstOrNull { !Modifier.isStatic(it.modifiers) && it.returnType == returnType }
            ?: throw NoSuchMethodException("${target.name} Types: $returnType")

    @Throws(IllegalAccessException::class, NoSuchMethodException::class, InvocationTargetException::class, InstantiationException::class)
    fun redirectRecordObject(source: Any, match: (Any?) -> Boolean, redirect: (Any?) -> Any?): Any {
        val fieldObjectMap = linkedMapOf<Field, Any?>()
        for (field in source.javaClass.declaredFields) {
            if (Modifier.isStatic(field.modifiers)) continue
            var value = handleAccessible(field).get(source)
            if (match(value)) value = redirect(value)
            fieldObjectMap[field] = value
        }
        val ctor = source.javaClass.getDeclaredConstructor(*fieldObjectMap.keys.map { it.type }.toTypedArray())
        return ctor.newInstance(*fieldObjectMap.values.toTypedArray())
    }
}
