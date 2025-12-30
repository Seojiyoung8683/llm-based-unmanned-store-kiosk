package com.kiosk.jarvis.model

data class OrderItem(
    val productId: String,
    val name: String,
    val unitPrice: Int,
    val quantity: Int,
    val imageResId: Int = 0
) {
    val lineTotal: Int get() = unitPrice * quantity
}
