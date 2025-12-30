package com.kiosk.jarvis.model

enum class ControlAction { TURN_ON, TURN_OFF, OPEN, CLOSE, RESTART }

data class RemoteControlAction(
    val deviceId: String,
    val action: ControlAction
)
