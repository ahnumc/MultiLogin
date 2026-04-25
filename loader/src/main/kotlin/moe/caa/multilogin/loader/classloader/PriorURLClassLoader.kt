package moe.caa.multilogin.loader.classloader

import java.net.URL
import java.net.URLClassLoader

/**
 * 优先类加载器
 */
open class PriorURLClassLoader(urls: Array<URL?>, parent: ClassLoader?, packageName: MutableSet<String>) :
    URLClassLoader(urls, parent), IExtURLClassLoader {
    private val packageName: MutableSet<String> = HashSet(packageName)

    override fun defineClass(name: String?, bytes: ByteArray): Class<*>? {
        return defineClass(name, bytes, 0, bytes.size)
    }

    @Throws(ClassNotFoundException::class)
    override fun loadClass(name: String, resolve: Boolean): Class<*>? {
        synchronized(getClassLoadingLock(name)) {
            var c = findLoadedClass(name)
            if (c == null) {
                if (containPrior(name)) {
                    try {
                        c = findClass(name)
                        if (resolve) resolveClass(c)
                        return c
                    } catch (ignored: ClassNotFoundException) {
                    }
                }
            }
        }
        return super.loadClass(name, resolve)
    }

    override fun addURL(url: URL?) {
        super.addURL(url)
    }

    override fun self(): URLClassLoader {
        return this
    }

    open fun containPrior(name: String): Boolean {
        for (s in packageName) {
            if (name.startsWith(s)) return true
        }
        return false
    }

    companion object {
        init {
            registerAsParallelCapable()
        }
    }
}
