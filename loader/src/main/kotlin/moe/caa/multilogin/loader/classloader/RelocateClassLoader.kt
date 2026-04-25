package moe.caa.multilogin.loader.classloader

import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.commons.ClassRemapper
import org.objectweb.asm.commons.Remapper
import java.io.ByteArrayOutputStream
import java.net.URL
import java.net.URLClassLoader

/**
 * 可以中途重定向包名的类加载器
 */
class RelocateClassLoader(
    urls: Array<URL?>,
    relocates: MutableSet<String?>,
    private val appendPrefix: String,
    parent: ClassLoader?
) : URLClassLoader(urls, parent), IExtURLClassLoader {
    private val relocates: Set<String?> = relocates.toSet()

    @Throws(ClassNotFoundException::class)
    override fun findClass(name: String): Class<*>? {
        if (name.startsWith(appendPrefix)) {
            try {
                val vanillaName = name.substring(appendPrefix.length)
                val path = vanillaName.replace('.', '/') + ".class"
                val inputStream = getResourceAsStream(path)
                val baos = ByteArrayOutputStream()
                var code: Int
                while ((inputStream!!.read().also { code = it }) != -1) {
                    baos.write(code)
                }
                var bytes = baos.toByteArray()

                val cr = ClassReader(bytes)
                val cw = ClassWriter(0)
                cr.accept(ClassRemapper(cw, AppendPrefixMapper()), ClassReader.EXPAND_FRAMES)
                bytes = cw.toByteArray()

                return defineClass(name, bytes, 0, bytes.size)
            } catch (ignored: Exception) {
            }
        }
        return super.findClass(name)
    }

    override fun addURL(url: URL?) {
        super.addURL(url)
    }

    override fun self(): URLClassLoader {
        return this
    }

    override fun defineClass(name: String?, bytes: ByteArray): Class<*>? {
        return defineClass(name, bytes, 0, bytes.size)
    }

    private inner class AppendPrefixMapper : Remapper() {
        override fun map(internalName: String): String? {
            for (relocate in relocates) {
                if (!internalName.startsWith(relocate!!.replace('.', '/'))) continue
                return appendPrefix.replace('.', '/') + internalName
            }
            return super.map(internalName)
        }
    }

    companion object {
        init {
            registerAsParallelCapable()
        }
    }
}
