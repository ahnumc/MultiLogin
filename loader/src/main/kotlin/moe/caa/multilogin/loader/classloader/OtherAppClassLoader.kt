package moe.caa.multilogin.loader.classloader

import java.net.URL
import java.net.URLClassLoader

/**
 * 另起的类加载器，与 AppClassLoader 同级并且互不干扰
 */
class OtherAppClassLoader(urls: Array<URL?>) : URLClassLoader(urls, extClassLoader), IExtURLClassLoader {
    override fun addURL(url: URL?) {
        super.addURL(url)
    }

    override fun self(): URLClassLoader {
        return this
    }

    override fun defineClass(name: String?, bytes: ByteArray): Class<*>? {
        return defineClass(name, bytes, 0, bytes.size)
    }

    companion object {
        val extClassLoader: ClassLoader? = getSystemClassLoader().parent

        init {
            registerAsParallelCapable()
        }
    }
}
