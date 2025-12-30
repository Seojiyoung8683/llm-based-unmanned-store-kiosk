package com.kiosk.jarvis.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.kiosk.jarvis.model.OrderItem

@Entity(tableName = "orders")
@TypeConverters(Converters::class)
data class OrderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val orderNumber: String,
    val orderedAtMillis: Long,
    val totalPrice: Int,
    val paymentMethod: String,
    val status: String,
    val items: List<OrderItem>
)
