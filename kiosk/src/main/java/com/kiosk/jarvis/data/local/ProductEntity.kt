// ProductEntity.kt
package com.kiosk.jarvis.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey val id: String,
    val name: String,
    val price: Int,
    val category: String,
    val barcode: String? = null,
    val imageUrl: String? = null,

    val stock: Int = 0,

    val updatedAt: Long = System.currentTimeMillis(),
    val deleted: Boolean = false
)
