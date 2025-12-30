package com.kiosk.jarvis.model

enum class DeviceType { KIOSK, LIGHTING, BLIND, DOOR, CAMERA, SCANNER, PAYMENT_TERMINAL }
enum class DeviceStatus { ONLINE, OFFLINE, WARNING, ERROR }

data class Device(
    val deviceId: String,
    val deviceName: String,
    val deviceType: DeviceType,
    val status: DeviceStatus,
    val storeId: String,
    val storeName: String,
    val temperature: Float? = null,
    val powerUsage: Float? = null,
    val networkLatency: Int? = null,
    val lastUpdate: Long
)
