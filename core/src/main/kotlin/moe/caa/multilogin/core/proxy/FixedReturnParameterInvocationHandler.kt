package moe.caa.multilogin.core.proxy

import moe.caa.multilogin.api.internal.function.BiConsumerFunction
import java.lang.reflect.InvocationHandler
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.lang.reflect.Modifier

class FixedReturnParameterInvocationHandler(
    private val handle: Any,
    private val match: (Method) -> Boolean,
    private val fixedFunc: BiConsumerFunction<Any, Array<Any?>, Any?>
) : InvocationHandler {

    init {
        val found = (handle.javaClass.declaredMethods + handle.javaClass.methods)
            .any { !Modifier.isStatic(it.modifiers) && match(it) }
        if (!found) throw RuntimeException("Methods may never be matched.")
    }

    @Throws(InvocationTargetException::class, IllegalAccessException::class)
    override fun invoke(proxy: Any?, method: Method, args: Array<Any?>): Any? {
        if (match(method)) return fixedFunc(handle, args)
        return method.invoke(handle, *args)
    }
}
