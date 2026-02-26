package net.ezpos.console.feature.release.service

import net.ezpos.console.feature.release.dto.ClientUpdateReportRequest
import net.ezpos.console.feature.release.entity.ClientUpdateReport
import net.ezpos.console.feature.release.repository.ClientUpdateReportRepository
import org.springframework.stereotype.Service

/**
 * 客户端更新上报服务。
 *
 * 负责将客户端上报的更新状态持久化到数据库。
 */
@Service
class ClientUpdateReportService(
    private val reportRepository: ClientUpdateReportRepository,
) {
    /**
     * 保存一条客户端更新上报记录。
     */
    fun save(request: ClientUpdateReportRequest) {
        val report = ClientUpdateReport(
            applicationCode = request.applicationCode,
            platform = request.platform,
            tenantId = request.tenantId,
            deviceId = request.deviceId,
            fromVersion = request.fromVersion,
            toVersion = request.toVersion,
            status = request.status,
            errorMessage = request.errorMessage,
        )
        reportRepository.save(report)
    }
}
