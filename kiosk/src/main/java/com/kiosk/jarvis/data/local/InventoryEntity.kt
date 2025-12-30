package com.kiosk.jarvis.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "inventory")
data class InventoryEntity(
    @PrimaryKey val productId: String,
    val stock: Int,
    val minThreshold: Int = 0,
    val location: String? = null,
    val updatedAt: Long = System.currentTimeMillis()
)
