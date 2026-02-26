package net.ezpos.console.feature.release.service

import net.ezpos.console.feature.release.entity.Release
import net.ezpos.console.feature.release.model.ReleaseRolloutType

/**
 * 灰度/投放命中判定器。
 *
 * 该对象仅负责“给定发布配置 + 客户端维度信息，是否命中该发布”的纯计算逻辑，
 * 不做任何 IO/数据库访问，便于复用与单元测试。
 *
 * 百分比投放的特点：
 * - 使用稳定的 key（`tenantId[:deviceId]:salt`）生成桶号，保证同一维度在同一 salt 下的命中结果稳定
 * - bucket 取值为 0..99，命中条件为 `bucket < percent`
 */
object RolloutDecider {
    /**
     * 判断指定租户（可选区分设备）是否命中该发布。
     *
     * @param release 发布配置
     * @param tenantId 租户 id（必填）
     * @param deviceId 设备 id（可选；提供后可将同一租户不同设备打散）
     */
    fun isIncluded(
        release: Release,
        tenantId: String,
        deviceId: String?,
    ): Boolean {
        return when (release.rolloutType) {
            ReleaseRolloutType.ALL -> true
            ReleaseRolloutType.WHITELIST -> WhitelistTenantsCodec.contains(release.whitelistTenants, tenantId)
            ReleaseRolloutType.PERCENT -> {
                val percent = (release.percent ?: 0).coerceIn(0, 100)
                if (percent <= 0) return false
                if (percent >= 100) return true

                val salt = (release.rolloutSalt?.takeIf { it.isNotBlank() } ?: release.version).trim()
                val key = buildString {
                    append(tenantId.trim())
                    if (!deviceId.isNullOrBlank()) {
                        append(':')
                        append(deviceId.trim())
                    }
                    append(':')
                    append(salt)
                }

                // String.hashCode() 在 JVM 中是确定性的；将其映射到 0..99 的桶空间用于百分比命中。
                val hash = key.hashCode()
                val bucket = ((hash.toLong() and 0xffffffffL) % 100).toInt()
                bucket < percent
            }
        }
    }
}

