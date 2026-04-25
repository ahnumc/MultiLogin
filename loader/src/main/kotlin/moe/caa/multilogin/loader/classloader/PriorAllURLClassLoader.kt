package moe.caa.multilogin.loader.classloader

import java.net.URL

/**
 * 所有优先类加载器
 */
class PriorAllURLClassLoader @JvmOverloads constructor(
    urls: Array<URL?>,
    parent: ClassLoader?,
    ignored: MutableSet<String> = HashSet()
) : PriorURLClassLoader(urls, parent, mutableSetOf()), IExtURLClassLoader {
    private val ignored: MutableSet<String> = HashSet(ignored)

    override fun containPrior(name: String): Boolean {
        return !containIgnore(name)
    }

    override fun defineClass(name: String?, bytes: ByteArray): Class<*>? {
        return defineClass(name, bytes, 0, bytes.size)
    }

    private fun containIgnore(name: String): Boolean {
        for (s in ignored) {
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
