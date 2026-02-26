package net.ezpos.console.feature.release.dto

import java.time.OffsetDateTime

/**
 * 客户端“检查更新”接口的响应体。
 *
 * 该响应采用“条件返回”的方式：
 * - 当 [updateAvailable] 为 `false`：其余字段通常为 `null`
 * - 当 [updateAvailable] 为 `true`：返回目标版本信息、最低支持版本、是否强制、下载信息等
 *
 * @property updateAvailable 是否存在可用更新
 * @property releaseId 命中的发布记录 id（无更新时可为空）
 * @property latestVersion 命中的最新版本号（无更新时可为空）
 * @property minSupportedVersion 最低支持版本号（无更新时可为空）
 * @property isForced 是否强制升级（无更新时可为空）
 * @property forceAfterAt 强制升级的生效时间（可为空）
 * @property releaseNotes 发布说明（可为空）
 * @property download 下载信息（可为空）
 */
data class ClientUpdateCheckResponse(
    val updateAvailable: Boolean,
    val releaseId: Long? = null,
    val latestVersion: String? = null,
    val minSupportedVersion: String? = null,
    val isForced: Boolean? = null,
    val forceAfterAt: OffsetDateTime? = null,
    val releaseNotes: String? = null,
    val download: DownloadInfo? = null,
) {
    /**
     * 客户端下载制品所需信息。
     *
     * @property url 下载地址
     * @property sha256 可选的内容校验和
     * @property fileSize 可选的文件大小（字节）
     */
    data class DownloadInfo(
        val url: String,
        val sha256: String? = null,
        val fileSize: Long? = null,
    )
}

