package moe.caa.multilogin.core.proxy

import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Modifier

class InterceptMethodInvocationHandler(
    private val handle: Any,
    private val match: (Method?) -> Boolean,
    private val redirect: (Method?, Array<Any?>?) -> Any?
) : InvocationHandler {

    init {
        val found = (handle.javaClass.declaredMethods + handle.javaClass.methods)
            .any { !Modifier.isStatic(it.modifiers) && match(it) }
        if (!found) throw RuntimeException("Methods may never be matched.")
    }

    @Throws(Throwable::class)
    override fun invoke(proxy: Any?, method: Method, args: Array<Any?>): Any? {
        if (match(method)) return redirect(method, args)
        return method.invoke(handle, *args)
    }
}
