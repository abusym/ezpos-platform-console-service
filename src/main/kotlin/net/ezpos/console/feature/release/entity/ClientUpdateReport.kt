package net.ezpos.console.feature.release.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.Table
import net.ezpos.console.common.entity.base.IdEntity

/**
 * 客户端更新上报记录。
 *
 * 用于记录客户端在执行更新过程中的状态（下载完成 / 安装完成 / 失败等），
 * 为后续统计分析提供原始数据。
 *
 * @property applicationCode 应用编码
 * @property platform 平台标识
 * @property tenantId 租户 id
 * @property deviceId 设备 id（可选）
 * @property fromVersion 更新前版本
 * @property toVersion 更新目标版本
 * @property status 上报状态（downloaded / installed / failed）
 * @property errorMessage 失败时的错误信息（可选）
 */
@Entity
@Table(
    name = "client_update_reports",
    indexes = [
        Index(name = "idx_client_update_reports_app_platform", columnList = "application_code, platform"),
        Index(name = "idx_client_update_reports_created_at", columnList = "created_at"),
    ],
)
class ClientUpdateReport(
    @Column(name = "application_code", nullable = false, length = 64)
    var applicationCode: String,

    @Column(name = "platform", nullable = false, length = 32)
    var platform: String,

    @Column(name = "tenant_id", nullable = false, length = 64)
    var tenantId: String,

    @Column(name = "device_id", nullable = true, length = 128)
    var deviceId: String? = null,

    @Column(name = "from_version", nullable = false, length = 32)
    var fromVersion: String,

    @Column(name = "to_version", nullable = false, length = 32)
    var toVersion: String,

    @Column(name = "status", nullable = false, length = 16)
    var status: String,

    @Column(name = "error_message", nullable = true, length = 1000)
    var errorMessage: String? = null,
) : IdEntity()
