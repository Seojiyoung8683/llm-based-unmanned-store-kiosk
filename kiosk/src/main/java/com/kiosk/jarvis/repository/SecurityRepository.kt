package com.kiosk.jarvis.repository

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

enum class AuditAction {
    PRICE_CHANGE, DEVICE_CONTROL, STOCK_ADJUSTMENT, PROMOTION_ACTIVATE, ALERT_RESOLVE, LOGIN, LOGOUT
}

enum class LogSeverity { INFO, WARNING, CRITICAL }

enum class UserRole { ADMIN, MANAGER, OPERATOR, STAFF }

data class AuditLog(
    val logId: String,
    val userId: String,
    val userName: String,
    val action: AuditAction,
    val targetType: String,
    val targetId: String,
    val targetName: String?,
    val details: String?,
    val ipAddress: String?,
    val timestamp: Long,
    val severity: LogSeverity
)

data class UserSession(
    val sessionId: String,
    val userId: String,
    val userName: String,
    val role: UserRole,
    val loginAt: Long,
    val lastSeenAt: Long,
    val ipAddress: String?,
    val userAgent: String?
)

class SecurityRepository {


    fun getAuditLogs(
        userId: String? = null,
        action: AuditAction? = null,
        startDate: Long? = null,
        endDate: Long? = null
    ): Flow<List<AuditLog>> = flow {
        delay(500)
        val now = System.currentTimeMillis()
        val logs = listOf(
            AuditLog("L001","U001","관리자", AuditAction.PRICE_CHANGE,       "Product","P001","홈런볼","가격 변경: 4000원 → 4500원","192.168.1.100", now - 2_592_000_000, LogSeverity.INFO),
            AuditLog("L002","U001","관리자", AuditAction.DEVICE_CONTROL,     "Device", "D002","강남점 조명","조명 켜기 명령 전송","192.168.1.100",  now -    86_400_000, LogSeverity.INFO),
            AuditLog("L003","U002","매니저", AuditAction.STOCK_ADJUSTMENT,  "Inventory","I002","카페라떼","재고 조정: +50개","192.168.1.105",     now -    43_200_000, LogSeverity.WARNING),
            AuditLog("L004","U001","관리자", AuditAction.PROMOTION_ACTIVATE,"Promotion","PROMO001","신년 특별 할인","프로모션 활성화","192.168.1.100", now -   172_800_000, LogSeverity.INFO),
            AuditLog("L005","U003","운영자", AuditAction.ALERT_RESOLVE,     "Alert", "A001","스캐너 오류","알림 해결 처리","192.168.1.110",       now -     3_600_000, LogSeverity.CRITICAL)
        )

        var filtered = logs
        if (userId   != null) filtered = filtered.filter { it.userId == userId }
        if (action   != null) filtered = filtered.filter { it.action == action }
        if (startDate!= null) filtered = filtered.filter { it.timestamp >= startDate }
        if (endDate  != null) filtered = filtered.filter { it.timestamp <= endDate }

        emit(filtered)
    }

    fun getActiveSessions(): Flow<List<UserSession>> = flow {
        delay(500)
        val now = System.currentTimeMillis()
        emit(
            listOf(
                UserSession("S001","U001","관리자", UserRole.ADMIN,   now - 7_200_000, now - 300_000, "192.168.1.100","Android 14"),
                UserSession("S002","U002","매니저", UserRole.MANAGER, now - 3_600_000, now - 600_000, "192.168.1.105","Android 13"),
                UserSession("S003","U003","운영자", UserRole.OPERATOR,now - 1_800_000, now - 120_000,"192.168.1.110","Android 14")
            )
        )
    }

    suspend fun logAction(
        userId: String,
        userName: String,
        action: AuditAction,
        targetType: String,
        targetId: String,
        targetName: String?,
        details: String?,
        severity: LogSeverity = LogSeverity.INFO
    ): Result<String> {
        delay(200)
        return Result.success("감사 로그가 기록되었습니다.")
    }
}
