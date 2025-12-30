package com.kiosk.jarvis.repository

import com.kiosk.jarvis.model.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class FleetRepository {

    fun getAllDevices(): Flow<List<Device>> = flow {
        delay(500)
        emit(
            listOf(
                Device(
                    deviceId = "D001",
                    deviceName = "강남점 키오스크 1",
                    deviceType = DeviceType.KIOSK,
                    status = DeviceStatus.ONLINE,
                    storeId = "S001",
                    storeName = "강남점",
                    temperature = 42.5f,
                    powerUsage = 85.2f,
                    networkLatency = 12,
                    lastUpdate = System.currentTimeMillis()
                ),
                Device(
                    deviceId = "D002",
                    deviceName = "강남점 조명",
                    deviceType = DeviceType.LIGHTING,
                    status = DeviceStatus.ONLINE,
                    storeId = "S001",
                    storeName = "강남점",
                    powerUsage = 120.5f,
                    lastUpdate = System.currentTimeMillis()
                ),
                Device(
                    deviceId = "D003",
                    deviceName = "강남점 블라인드",
                    deviceType = DeviceType.BLIND,
                    status = DeviceStatus.ONLINE,
                    storeId = "S001",
                    storeName = "강남점",
                    lastUpdate = System.currentTimeMillis()
                ),
                Device(
                    deviceId = "D004",
                    deviceName = "홍대점 키오스크 1",
                    deviceType = DeviceType.KIOSK,
                    status = DeviceStatus.WARNING,
                    storeId = "S002",
                    storeName = "홍대점",
                    temperature = 58.3f,
                    powerUsage = 92.1f,
                    networkLatency = 45,
                    lastUpdate = System.currentTimeMillis()
                ),
                Device(
                    deviceId = "D005",
                    deviceName = "홍대점 스캐너",
                    deviceType = DeviceType.SCANNER,
                    status = DeviceStatus.ERROR,
                    storeId = "S002",
                    storeName = "홍대점",
                    networkLatency = 250,
                    lastUpdate = System.currentTimeMillis()
                ),
                Device(
                    deviceId = "D006",
                    deviceName = "판교점 카메라 1",
                    deviceType = DeviceType.CAMERA,
                    status = DeviceStatus.OFFLINE,
                    storeId = "S003",
                    storeName = "판교점",
                    lastUpdate = System.currentTimeMillis() - 300_000
                )
            )
        )
    }

    /** 매장별 디바이스 조회  */
    fun getDevicesByStore(storeId: String): Flow<List<Device>> = flow {
        getAllDevices().collect { devices -> emit(devices.filter { it.storeId == storeId }) }
    }

    fun getStoreSchedules(): Flow<List<StoreSchedule>> = flow {
        delay(500)
        emit(
            listOf(
                StoreSchedule("SCH001","S001","강남점","06:00","23:00", nightMode = true,  powerSaveMode = true,  autoShutdown = false),
                StoreSchedule("SCH002","S002","홍대점","07:00","22:00", nightMode = false, powerSaveMode = true,  autoShutdown = true),
                StoreSchedule("SCH003","S003","판교점","08:00","20:00", nightMode = true,  powerSaveMode = false, autoShutdown = false),
                StoreSchedule("SCH004","S004","잠실점","06:30","23:30", nightMode = true,  powerSaveMode = true,  autoShutdown = false)
            )
        )
    }

    fun getAlerts(includeResolved: Boolean = false): Flow<List<Alert>> = flow {
        delay(500)
        val list = listOf(
            Alert(
                alertId = "A001", storeId = "S002", storeName = "강남점",
                deviceId = "D005", deviceName = "스캐너",
                type = AlertType.SCANNER_ERROR, severity = AlertSeverity.CRITICAL,
                message = "스캐너 미응답 - 즉시 확인 필요",
                timestamp = System.currentTimeMillis() - 600_000, isResolved = false
            ),
            Alert(
                alertId = "A002", storeId = "S003", storeName = "강남점",
                deviceId = "D006", deviceName = "카메라 1",
                type = AlertType.CAMERA_DISCONNECTED, severity = AlertSeverity.WARNING,
                message = "카메라 연결 끊김",
                timestamp = System.currentTimeMillis() - 300_000, isResolved = false
            ),
            Alert(
                alertId = "A003", storeId = "S002", storeName = "홍대점",
                deviceId = null, deviceName = null,
                type = AlertType.PAYMENT_FAILURE, severity = AlertSeverity.WARNING,
                message = "결제 실패율 15% 증가",
                timestamp = System.currentTimeMillis() - 1_800_000, isResolved = false
            ),
            Alert(
                alertId = "A004", storeId = "S001", storeName = "강남점",
                deviceId = "D001", deviceName = "키오스크 1",
                type = AlertType.TEMPERATURE_ABNORMAL, severity = AlertSeverity.INFO,
                message = "온도 상승 감지 (58°C)",
                timestamp = System.currentTimeMillis() - 3_600_000, isResolved = true
            )
        )
        emit(if (includeResolved) list else list.filter { !it.isResolved })
    }

    suspend fun executeRemoteControl(action: RemoteControlAction): Result<String> {
        delay(1_000)

        val target = mapDeviceIdToLabel(action.deviceId)

        val actionText = when (action.action) {
            ControlAction.TURN_ON  -> "켰습니다"
            ControlAction.TURN_OFF -> "껐습니다"
            ControlAction.OPEN     -> "열었습니다"
            ControlAction.CLOSE    -> "닫았습니다"
            else                   -> "제어했습니다"
        }

        val message = "${target}을(를) $actionText."
        return Result.success(message)
    }

    suspend fun updateSchedule(schedule: StoreSchedule): Result<String> {
        delay(500)
        return Result.success("스케줄이 업데이트되었습니다. (${schedule.storeName})")
    }

    suspend fun resolveAlert(alertId: String): Result<String> {
        delay(300)
        return Result.success("알림이 해결 처리되었습니다. ($alertId)")
    }

    private fun mapDeviceIdToLabel(deviceId: String): String = when (deviceId) {
        "ENV_LIGHTING"    -> "조명"
        "ENV_DOOR"        -> "출입문"
        "ENV_AC"          -> "에어컨"
        "ENV_BLIND"       -> "블라인드"
        "ENV_SPEAKER"     -> "스피커"
        "ENV_HUMIDIFIER"  -> "가습기"
        else              -> "디바이스($deviceId)"
    }
}
