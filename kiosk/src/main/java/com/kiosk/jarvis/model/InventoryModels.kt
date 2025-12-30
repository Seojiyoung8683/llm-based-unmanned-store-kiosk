package com.kiosk.jarvis.model

data class InventoryItem(
    val inventoryId: String,
    val productId: String,
    val productName: String,
    val storeId: String,
    val storeName: String,
    val currentStock: Int,
    val minThreshold: Int,
    val maxCapacity: Int,
    val lastRefillDate: Long,
    val expiryDate: Long? = null,
    val status: InventoryStatus
)

enum class InventoryStatus {
    NORMAL,
    LOW_STOCK,
    OUT_OF_STOCK,
    EXPIRING_SOON,
    EXPIRED
}

data class RefillTask(
    val taskId: String,
    val storeId: String,
    val storeName: String,
    val productId: String,
    val productName: String,
    val currentStock: Int,
    val refillAmount: Int,
    val priority: RefillPriority,
    val displayPosition: String?,
    val status: RefillStatus,
    val createdAt: Long,
    val completedAt: Long? = null
)

enum class RefillPriority {
    LOW,
    MEDIUM,
    HIGH,
    URGENT
}

enum class RefillStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED
}

data class StockMovement(
    val movementId: String,
    val storeId: String,
    val productId: String,
    val productName: String,
    val movementType: MovementType,
    val quantity: Int,
    val reason: String?,
    val timestamp: Long,
    val performedBy: String
)

enum class MovementType {
    INBOUND,
    OUTBOUND,
    REFILL,
    RETURN,
    ADJUSTMENT,
    EXPIRED
}

data class VendorDelivery(
    val deliveryId: String,
    val vendorName: String,
    val storeId: String,
    val storeName: String,
    val expectedDate: Long,
    val actualDate: Long? = null,
    val items: List<DeliveryItem>,
    val status: DeliveryStatus
)

data class DeliveryItem(
    val productId: String,
    val productName: String,
    val expectedQuantity: Int,
    val receivedQuantity: Int? = null,
    val defectQuantity: Int? = null,
    val expiryDate: Long? = null
)

enum class DeliveryStatus {
    SCHEDULED,
    IN_TRANSIT,
    DELIVERED,
    INSPECTED,
    COMPLETED
}
