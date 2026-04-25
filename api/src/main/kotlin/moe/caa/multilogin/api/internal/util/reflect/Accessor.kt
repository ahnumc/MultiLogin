package moe.caa.multilogin.api.internal.util.reflect

import org.jetbrains.annotations.ApiStatus
import java.lang.reflect.Constructor
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.Type

@ApiStatus.Internal
class Accessor(val classHandle: Class<*>) {
    private fun methods(declared: Boolean) = if (declared) classHandle.declaredMethods else classHandle.methods
    private fun fields(declared: Boolean) = if (declared) classHandle.declaredFields else classHandle.fields
    private fun constructors(declared: Boolean) = if (declared) classHandle.declaredConstructors else classHandle.constructors
    private fun ctx(declared: Boolean) = "${classHandle.name}(dedicated = $declared)"

    fun findAllMethods(declared: Boolean, predicate: (Method) -> Boolean): List<Method> = methods(declared).filter(predicate)
    fun findAllFields(declared: Boolean, predicate: (Field) -> Boolean): List<Field> = fields(declared).filter(predicate)
    fun findAllConstructors(declared: Boolean, predicate: (Constructor<*>) -> Boolean): List<Constructor<*>> = constructors(declared).filter(predicate)

    @Throws(NoSuchMethodException::class)
    fun findFirstMethod(declared: Boolean, predicate: (Method) -> Boolean, exceptionMessage: String?): Method =
        methods(declared).firstOrNull(predicate) ?: throw NoSuchMethodException(exceptionMessage)

    @Throws(NoSuchFieldException::class)
    fun findFirstField(declared: Boolean, predicate: (Field) -> Boolean, exceptionMessage: String?): Field =
        fields(declared).firstOrNull(predicate) ?: throw NoSuchFieldException(exceptionMessage)

    @Throws(NoSuchConstructorException::class)
    fun findFirstConstructors(declared: Boolean, predicate: (Constructor<*>) -> Boolean, exceptionMessage: String?): Constructor<*> =
        constructors(declared).firstOrNull(predicate) ?: throw NoSuchConstructorException(exceptionMessage)

    @Throws(NoSuchMethodException::class)
    fun findFirstMethodByName(declared: Boolean, name: String?): Method =
        findFirstMethod(declared, { it.name == name }, "${ctx(declared)} -> $name")

    @Throws(NoSuchMethodException::class)
    fun findFirstMethodByParameterTypes(declared: Boolean, types: Array<Type?>?): Method =
        findFirstMethod(declared, { types.contentEquals(it.parameterTypes) }, "${ctx(declared)} -> ${types.contentToString()}")

    @Throws(NoSuchMethodException::class)
    fun findFirstMethodByReturnType(declared: Boolean, returnType: Type?): Method =
        findFirstMethod(declared, { it.returnType == returnType }, "${ctx(declared)} -> returnType = $returnType")

    @Throws(NoSuchFieldException::class)
    fun findFirstFieldByName(declared: Boolean, name: String?): Field =
        findFirstField(declared, { it.name == name }, "${ctx(declared)} -> $name")

    @Throws(NoSuchFieldException::class)
    fun findFirstFieldByType(declared: Boolean, fieldType: Type?): Field =
        findFirstField(declared, { it.type == fieldType }, "${ctx(declared)} -> $fieldType")

    @Throws(NoSuchConstructorException::class)
    fun findFirstConstructorByParameterTypes(declared: Boolean, types: Array<Type?>?): Constructor<*> =
        findFirstConstructors(declared, { it.parameterTypes.contentEquals(types) }, "${ctx(declared)} -> ${types.contentToString()}")
}
