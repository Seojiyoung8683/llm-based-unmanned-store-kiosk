package com.kiosk.jarvis.model

data class SalesData(
    val date: String,
    val revenue: Double,
    val transactions: Int,
    val averageTransaction: Double
)

data class StoreSummary(
    val storeId: String,
    val storeName: String,
    val todayRevenue: Double,
    val todayTransactions: Int,
    val status: StoreStatus
)

enum class StoreStatus {
    ONLINE,
    OFFLINE,
    MAINTENANCE,
    ERROR
}

data class SalesMetrics(
    val totalRevenue: Double,
    val totalTransactions: Int,
    val averageTransaction: Double,
    val revenueChange: Double, // 전일 대비 증감률
    val transactionChange: Double
)

data class TopProduct(
    val productId: String,
    val productName: String,
    val salesCount: Int,
    val revenue: Double
)
