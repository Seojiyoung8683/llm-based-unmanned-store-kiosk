package com.kiosk.jarvis.model

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

enum class AuditAction {
    CREATE,
    UPDATE,
    DELETE,
    VIEW,
    LOGIN,
    LOGOUT,
    PRICE_CHANGE,
    STOCK_ADJUSTMENT,
    DEVICE_CONTROL,
    PROMOTION_ACTIVATE,
    PROMOTION_DEACTIVATE,
    SCHEDULE_UPDATE,
    ALERT_RESOLVE
}

enum class LogSeverity {
    INFO,
    WARNING,
    CRITICAL
}

data class UserSession(
    val sessionId: String,
    val userId: String,
    val userName: String,
    val role: UserRole,
    val loginTime: Long,
    val lastActivity: Long,
    val ipAddress: String,
    val deviceInfo: String
)

enum class UserRole {
    ADMIN,
    MANAGER,
    OPERATOR,
    VIEWER
}

data class SecuritySettings(
    val requireTwoFactor: Boolean = false,
    val sessionTimeout: Int = 3600,
    val maxLoginAttempts: Int = 5,
    val passwordMinLength: Int = 8,
    val passwordRequireSpecialChar: Boolean = true,
    val auditLogRetentionDays: Int = 90
)
