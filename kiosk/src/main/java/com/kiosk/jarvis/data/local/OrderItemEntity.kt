package com.kiosk.jarvis.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "order_items",
    indices = [
        Index(value = ["orderedAtMillis"], name = "idx_order_items_date"),
        Index(value = ["productId"],      name = "idx_order_items_product"),
        Index(value = ["orderId"],        name = "idx_order_items_order")
    ]
)
data class OrderItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val orderId: Long,
    val productId: String,
    val quantity: Int,
    val unitPrice: Int,
    val orderedAtMillis: Long
)
