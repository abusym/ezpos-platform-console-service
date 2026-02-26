package net.ezpos.console.feature.release.model

/**
 * 语义化版本号（Semantic Versioning）模型，格式为 `major.minor.patch`。
 *
 * 该类型实现了 [Comparable]，比较规则遵循语义化版本的常见排序：先比 `major`，再比 `minor`，最后比 `patch`。
 *
 * @property major 主版本号：发生不兼容变更时递增
 * @property minor 次版本号：向后兼容地新增功能时递增
 * @property patch 修订号：向后兼容的问题修复时递增
 */
data class SemVer(
    val major: Int,
    val minor: Int,
    val patch: Int,
) : Comparable<SemVer> {
    /**
     * 与另一个版本号进行排序比较。
     *
     * @return 小于 0 表示当前版本更小；等于 0 表示相等；大于 0 表示当前版本更大
     */
    override fun compareTo(other: SemVer): Int {
        val majorCmp = major.compareTo(other.major)
        if (majorCmp != 0) return majorCmp
        val minorCmp = minor.compareTo(other.minor)
        if (minorCmp != 0) return minorCmp
        return patch.compareTo(other.patch)
    }

    /**
     * 将版本号序列化为 `major.minor.patch` 字符串。
     */
    override fun toString(): String = "$major.$minor.$patch"

    companion object {
        private val pattern = Regex("""^(\d+)\.(\d+)\.(\d+)$""")

        /**
         * 将字符串解析为 [SemVer]。
         *
         * 解析规则：
         * - 仅接受严格的三段式数字版本：`x.y.z`
         * - 会先 [String.trim] 去除首尾空白
         *
         * @param value 待解析的版本字符串
         * @return 解析成功返回 [SemVer]；格式不合法或任一段无法转为整数时返回 `null`
         */
        fun parse(value: String): SemVer? {
            val m = pattern.matchEntire(value.trim()) ?: return null
            return SemVer(
                major = m.groupValues[1].toIntOrNull() ?: return null,
                minor = m.groupValues[2].toIntOrNull() ?: return null,
                patch = m.groupValues[3].toIntOrNull() ?: return null,
            )
        }
    }
}

