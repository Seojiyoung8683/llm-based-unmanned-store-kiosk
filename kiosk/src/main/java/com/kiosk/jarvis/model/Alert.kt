package com.kiosk.jarvis.model


data class Alert(
    val alertId: String,
    val storeId: String,
    val storeName: String,
    val deviceId: String?,
    val deviceName: String?,
    val type: AlertType,
    val severity: AlertSeverity,
    val message: String,
    val timestamp: Long,
    val isResolved: Boolean
)
enum class AlertType { SCANNER_ERROR, CAMERA_DISCONNECTED, PAYMENT_FAILURE, TEMPERATURE_ABNORMAL }
enum class AlertSeverity { INFO, WARNING, CRITICAL }
