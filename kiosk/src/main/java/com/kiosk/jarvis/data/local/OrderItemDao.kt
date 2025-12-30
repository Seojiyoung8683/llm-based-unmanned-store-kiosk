package com.kiosk.jarvis.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface OrderItemDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<OrderItemEntity>)

    @Query(
        """
        SELECT 
            strftime('%Y-%m-%d', datetime(orderedAtMillis/1000,'unixepoch','localtime')) AS d,
            SUM(quantity * unitPrice) AS revenue,
            COUNT(DISTINCT orderId)   AS orders
        FROM order_items
        GROUP BY d
        ORDER BY d
        """
    )
    fun observeDailySales(): Flow<List<DailySalesRow>>

    @Query(
        """
        SELECT 
            strftime('%Y-%m-%d', datetime(oi.orderedAtMillis/1000,'unixepoch','localtime')) AS d,
            oi.productId          AS productId,
            COALESCE(p.name,'')   AS productName,
            SUM(oi.quantity)      AS quantity,
            SUM(oi.quantity * oi.unitPrice) AS revenue
        FROM order_items oi
        LEFT JOIN products p ON p.id = oi.productId
        GROUP BY d, productId
        ORDER BY d ASC, revenue DESC
        """
    )
    fun observeDailyProductSales(): Flow<List<DailyProductSalesRow>>

    @Query(
        """
        SELECT 
            oi.productId          AS productId,
            COALESCE(p.name,'')   AS productName,
            SUM(oi.quantity)      AS quantity,
            SUM(oi.quantity * oi.unitPrice) AS revenue
        FROM order_items oi
        LEFT JOIN products p ON p.id = oi.productId
        GROUP BY oi.productId
        ORDER BY revenue DESC
        LIMIT :limit
        """
    )
    fun observeTopProducts(limit: Int = 5): Flow<List<TopProductRow>>
}

// === DashboardRepository 에서 사용하는 DTO들 ===

data class DailySalesRow(
    val d: String,
    val revenue: Double,
    val orders: Int
)

data class DailyProductSalesRow(
    val d: String,
    val productId: String,
    val productName: String,
    val quantity: Int,
    val revenue: Double
)

data class TopProductRow(
    val productId: String,
    val productName: String,
    val quantity: Int,
    val revenue: Double
)
