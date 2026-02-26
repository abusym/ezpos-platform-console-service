package net.ezpos.console.feature.release.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.time.OffsetDateTime

/**
 * 客户端"检查更新"接口的响应体。
 *
 * 该响应采用"条件返回"的方式：
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
@Schema(description = "客户端检查更新响应")
data class ClientUpdateCheckResponse(
    @Schema(description = "是否存在可用更新")
    val updateAvailable: Boolean,
    @Schema(description = "发布记录 ID")
    val releaseId: Long? = null,
    @Schema(description = "最新版本号", example = "1.2.0")
    val latestVersion: String? = null,
    @Schema(description = "最低支持版本号", example = "1.0.0")
    val minSupportedVersion: String? = null,
    @Schema(description = "是否强制升级")
    val isForced: Boolean? = null,
    @Schema(description = "强制升级生效时间")
    val forceAfterAt: OffsetDateTime? = null,
    @Schema(description = "发布说明")
    val releaseNotes: String? = null,
    @Schema(description = "下载信息")
    val download: DownloadInfo? = null,
) {
    /**
     * 客户端下载制品所需信息。
     *
     * @property url 下载地址
     * @property sha256 可选的内容校验和
     * @property fileSize 可选的文件大小（字节）
     */
    @Schema(description = "下载信息")
    data class DownloadInfo(
        @Schema(description = "下载地址")
        val url: String,
        @Schema(description = "SHA-256 校验和")
        val sha256: String? = null,
        @Schema(description = "文件大小（字节）")
        val fileSize: Long? = null,
    )
}
