package moe.caa.multilogin.core.semver

/**
 * 语义化版本号处理工具
 */
class SemVersion private constructor(
    private val major: Int,
    private val minor: Int,
    private val patch: Int,
    private val suffixes: VersionSuffix,
    private val suffixesBd: Int
) {
    override fun toString(): String {
        if (suffixes == VersionSuffix.NONE) return "$major.$minor.$patch"
        return "$major.$minor.$patch-${suffixes.name}.$suffixesBd"
    }

    fun needUpgrade(version: SemVersion): Boolean {
        if (version.suffixes.mj < suffixes.mj) return false
        if (version.major == major && version.minor == minor && version.patch == patch) {
            if (version.suffixes.mj > suffixes.mj) return true
            if (version.suffixesBd > suffixesBd) return true
        }
        return needUpgradeIgnoreSuffixes(version)
    }

    fun needUpgradeIgnoreSuffixes(version: SemVersion): Boolean {
        return version.major >= major && version.minor >= minor && version.patch > patch
    }

    internal enum class VersionSuffix(val mj: Int) {
        NONE(3),
        RC(2),
        BETA(1),
        ALPHA(0)
    }

    companion object {
        fun of(version: String): SemVersion? {
            if (version.isEmpty()) return null
            if (version.lowercase().startsWith("build_")) return null
            val split = version.split("-")
            val mmp = split[0].split(".")
            if (mmp.size != 3) return null
            if (split.size == 1) {
                return SemVersion(
                    mmp[0].toInt(), mmp[1].toInt(), mmp[2].toInt(),
                    VersionSuffix.NONE, -1
                )
            }
            if (split.size == 2) {
                val suffixParts = split[1].split(".")
                if (suffixParts.size != 2) return null
                return SemVersion(
                    mmp[0].toInt(), mmp[1].toInt(), mmp[2].toInt(),
                    VersionSuffix.valueOf(suffixParts[0]), suffixParts[1].toInt()
                )
            }
            return null
        }
    }
}
