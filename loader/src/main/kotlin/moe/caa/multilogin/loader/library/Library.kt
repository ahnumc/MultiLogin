package moe.caa.multilogin.loader.library

/**
 * 代表一个依赖
 */
data class Library(
    val group: String?,
    val name: String?,
    val version: String?
) {
    val fileName: String
        get() = "$name-$version.jar"

    val downloadUrl: String
        get() = group!!.replace(".", "/") + "/" + name + "/" + version + "/" + name + "-" + version + ".jar"

    companion object {
        fun of(value: String?, split: String): Library {
            val args = value!!.split(split.toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            return Library(args[0], args[1], args[2])
        }
    }
}
