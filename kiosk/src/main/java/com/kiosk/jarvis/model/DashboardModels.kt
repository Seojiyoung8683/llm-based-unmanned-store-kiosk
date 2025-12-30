package com.kiosk.jarvis.model.dashboard

data class SalesMetrics(
    val totalRevenue: Double,
    val totalOrders: Int,
    val avgOrderValue: Double,
    val conversionRate: Double,
    val changeRevenuePct: Double? = null,
    val changeOrdersPct: Double? = null
)

data class StoreSummary(
    val storeId: String,
    val storeName: String,
    val todayRevenue: Double,
    val todayOrders: Int,
    val onlineDevices: Int,
    val offlineDevices: Int
)

data class SalesData(
    val dateMillis: Long,
    val revenue: Double,
    val orders: Int
)

data class TopProduct(
    val productId: String,
    val name: String,
    val quantity: Int,
    val revenue: Double,
    val imageUrl: String? = null
)
