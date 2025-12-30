package com.kiosk.jarvis.model

data class StoreSchedule(
    val scheduleId: String,
    val storeId: String,
    val storeName: String,
    val openTime: String,   // "06:00"
    val closeTime: String,  // "23:00"
    val nightMode: Boolean = false,
    val powerSaveMode: Boolean = false,
    val autoShutdown: Boolean = false
)
