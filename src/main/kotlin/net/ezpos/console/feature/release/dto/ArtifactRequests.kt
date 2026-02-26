package net.ezpos.console.feature.release.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size

/**
 * 标记制品信息已就绪（例如上传完成、可下载）时使用的请求体。
 *
 * 该请求体通常用于将制品的 key、下载 URL、校验和、大小等信息写回发布记录。
 * 字段均为可选，以便支持不同对象存储/上传链路的能力差异。
 */
@Schema(description = "制品信息完成请求")
data class CompleteArtifactRequest(
    @field:Size(max = 512)
    @Schema(description = "制品存储 key")
    val artifactKey: String? = null,

    @field:Size(max = 2000)
    @Schema(description = "制品下载地址")
    val artifactUrl: String? = null,

    @field:Size(max = 128)
    @Schema(description = "制品 SHA-256 校验和")
    val sha256: String? = null,

    @Schema(description = "文件大小（字节）")
    val fileSize: Long? = null,
)

/**
 * 获取制品预签名上传/下载地址的响应体。
 *
 * 当前实现可能尚未接入对象存储预签名能力，因此保留 `notImplemented` 与 `message` 供前端提示。
 *
 * @property notImplemented 是否未实现（默认 `true`）
 * @property message 说明信息（用于提示或调试）
 */
@Schema(description = "制品预签名响应")
data class ArtifactPresignResponse(
    @Schema(description = "是否未实现")
    val notImplemented: Boolean = true,
    @Schema(description = "说明信息")
    val message: String,
)
