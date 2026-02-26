package net.ezpos.console.feature.release.service

/**
 * 发布白名单租户集合的编解码工具。
 *
 * 设计目标：
 * - **存储友好**：在数据库中用单个文本字段保存租户列表
 * - **可读可改**：采用逗号分隔，便于人工排查与手工编辑
 * - **去噪**：编码/解码均会 trim、去空、去重、排序，得到稳定表示
 */
object WhitelistTenantsCodec {
    /**
     * 将租户列表编码为数据库字段值。
     *
     * @param values 租户 id 列表（允许为 `null`）
     * @return 若输入为空/仅包含空白项，则返回 `null`；否则返回按逗号分隔的规范化字符串
     */
    fun encode(values: List<String>?): String? {
        val normalized = values
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?.distinct()
            ?.sorted()
            ?: emptyList()
        if (normalized.isEmpty()) return null
        return normalized.joinToString(separator = ",")
    }

    /**
     * 将数据库字段值解码为租户列表。
     *
     * @param value 数据库中的编码字符串（允许为 `null`）
     * @return 规范化后的租户列表（trim/去空/去重/排序）；若输入为空则返回空列表
     */
    fun decode(value: String?): List<String> =
        value
            ?.split(',')
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?.distinct()
            ?.sorted()
            ?: emptyList()

    /**
     * 判断编码字符串中是否包含指定租户。
     *
     * @param value 编码后的白名单字符串（允许为 `null`）
     * @param tenantId 待匹配的租户 id
     * @return 当 `tenantId` 为空白时返回 `false`；否则按解码后的列表判定是否包含
     */
    fun contains(value: String?, tenantId: String): Boolean {
        val needle = tenantId.trim()
        if (needle.isEmpty()) return false
        return decode(value).contains(needle)
    }
}

