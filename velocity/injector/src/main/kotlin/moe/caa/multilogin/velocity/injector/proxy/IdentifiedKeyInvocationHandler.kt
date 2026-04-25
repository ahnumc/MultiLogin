package moe.caa.multilogin.velocity.injector.proxy

import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method

/**
 * IdentifiedKey 代理，始终让签名数据有效
 */
class IdentifiedKeyInvocationHandler(private val obj: Any?) : InvocationHandler {
    @Throws(Throwable::class)
    override fun invoke(proxy: Any?, method: Method, args: Array<Any?>): Any? {
        if (method.getName() == "hasExpired") {
            return false
        }
        if (method.getName() == "isSignatureValid") {
            return true
        }
        if (method.getName() == "internalAddHolder") {
            return true
        }
        return method.invoke(obj, *args)
    }
}
