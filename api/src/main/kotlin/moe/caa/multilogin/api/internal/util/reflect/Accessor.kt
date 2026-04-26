package moe.caa.multilogin.api.internal.util.reflect

import org.jetbrains.annotations.ApiStatus
import java.lang.reflect.Constructor
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.Type
import java.util.function.Predicate

@ApiStatus.Internal
class Accessor(val classHandle: Class<*>) {
    private fun methods(declared: Boolean) = if (declared) classHandle.declaredMethods else classHandle.methods
    private fun fields(declared: Boolean) = if (declared) classHandle.declaredFields else classHandle.fields
    private fun constructors(declared: Boolean) = if (declared) classHandle.declaredConstructors else classHandle.constructors
    private fun ctx(declared: Boolean) = "${classHandle.name}(dedicated = $declared)"

    fun findAllMethods(declared: Boolean, predicate: Predicate<Method>): List<Method> =
        methods(declared).filter(predicate::test)

    fun findAllFields(declared: Boolean, predicate: Predicate<Field>): List<Field> =
        fields(declared).filter(predicate::test)

    fun findAllConstructors(declared: Boolean, predicate: Predicate<Constructor<*>>): List<Constructor<*>> =
        constructors(declared).filter(predicate::test)

    @Throws(NoSuchMethodException::class)
    fun findFirstMethod(declared: Boolean, predicate: Predicate<Method>, exceptionMessage: String?): Method =
        methods(declared).firstOrNull(predicate::test) ?: throw NoSuchMethodException(exceptionMessage)

    @Throws(NoSuchFieldException::class)
    fun findFirstField(declared: Boolean, predicate: Predicate<Field>, exceptionMessage: String?): Field =
        fields(declared).firstOrNull(predicate::test) ?: throw NoSuchFieldException(exceptionMessage)

    @Throws(NoSuchConstructorException::class)
    fun findFirstConstructors(
        declared: Boolean,
        predicate: Predicate<Constructor<*>>,
        exceptionMessage: String?
    ): Constructor<*> =
        constructors(declared).firstOrNull(predicate::test) ?: throw NoSuchConstructorException(exceptionMessage)

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
