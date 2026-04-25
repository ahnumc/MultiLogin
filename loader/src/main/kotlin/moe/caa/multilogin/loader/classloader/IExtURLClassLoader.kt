package moe.caa.multilogin.loader.classloader

import java.net.URL
import java.net.URLClassLoader

/**
 * 表示一个插件的类加载器
 */
interface IExtURLClassLoader {
    fun addURL(url: URL?)
    fun self(): URLClassLoader?
    fun defineClass(name: String?, bytes: ByteArray): Class<*>?
}
