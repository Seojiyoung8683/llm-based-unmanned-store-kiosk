package com.kiosk.jarvis.repository

import android.content.Context
import com.kiosk.jarvis.data.local.JarvisDatabase
import com.kiosk.jarvis.model.dashboard.SalesData
import com.kiosk.jarvis.model.dashboard.SalesMetrics
import com.kiosk.jarvis.model.dashboard.StoreSummary
import com.kiosk.jarvis.model.dashboard.TopProduct
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DashboardRepository(
    private val context: Context
) {
    private fun db() = JarvisDatabase.get(context)

    fun getKioskProviderStatus(): Flow<Boolean> = flow {
        emit(true)
    }

    // ────────────────────────────────
    // 1) 매출 지표: orders 테이블 기준
    // ────────────────────────────────
    fun getSalesMetrics(): Flow<SalesMetrics> =
        db().orderDao().observeAll().map { orders ->
            val totalRevenue = orders.sumOf { it.totalPrice.toDouble() }
            val totalOrders  = orders.size
            val avgOrder     = if (totalOrders > 0) totalRevenue / totalOrders else 0.0

            SalesMetrics(
                totalRevenue     = totalRevenue,
                totalOrders      = totalOrders,
                avgOrderValue    = avgOrder,
                conversionRate   = 0.0,
                changeRevenuePct = 0.0,   // 전일 대비 증감률 등
                changeOrdersPct  = 0.0
            )
        }

    // ────────────────────────────────
    // 2) 매장 요약: 오늘 매출/거래 건수
    // ────────────────────────────────
    fun getStoreSummaries(): Flow<List<StoreSummary>> =
        db().orderDao().observeAll().map { orders ->
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val todayStr   = dateFormat.format(Date())

            val todayOrdersList = orders.filter { order ->
                val orderDate = dateFormat.format(Date(order.orderedAtMillis))
                orderDate == todayStr
            }

            val todayRevenue = todayOrdersList.sumOf { it.totalPrice.toDouble() }
            val todayOrders  = todayOrdersList.size

            listOf(
                StoreSummary(
                    storeId        = "S001",
                    storeName      = "무인점",
                    todayRevenue   = todayRevenue,
                    todayOrders    = todayOrders,
                    onlineDevices  = 0,
                    offlineDevices = 0
                )
            )
        }

    // ────────────────────────────────
    // 3) 일별 매출 차트: orders 기준 집계
    // ────────────────────────────────
    fun getDailySales(days: Int = 7): Flow<List<SalesData>> =
        db().orderDao().observeAll().map { orders ->
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

            val grouped: Map<String, List<com.kiosk.jarvis.data.local.OrderEntity>> =
                orders.groupBy { order ->
                    dateFormat.format(Date(order.orderedAtMillis))
                }

            val sortedKeys  = grouped.keys.sorted()
            val targetKeys  = sortedKeys.takeLast(days)

            targetKeys.map { d ->
                val dailyOrders = grouped[d].orEmpty()
                val revenue     = dailyOrders.sumOf { it.totalPrice.toDouble() }
                val count       = dailyOrders.size
                val dateMillis  = dateFormat.parse(d)?.time ?: 0L

                SalesData(
                    dateMillis = dateMillis,
                    revenue    = revenue,
                    orders     = count
                )
            }
        }

    // ────────────────────────────────
    // 4) 인기 상품 TOP N: order_items 기준
    // ────────────────────────────────
    fun getTopProducts(limit: Int = 5): Flow<List<TopProduct>> =
        db().orderItemDao().observeTopProducts(limit).map { rows ->
            rows.map { r ->
                TopProduct(
                    productId = r.productId,
                    name      = r.productName,
                    quantity  = r.quantity,
                    revenue   = r.revenue,
                    imageUrl  = null
                )
            }
        }

    // ────────────────────────────────
    // 5) 일별 · 상품별 매출: order_items 기준
    // ────────────────────────────────
    fun getDailyProductSales(): Flow<List<DailyProductSalesUi>> =
        db().orderItemDao().observeDailyProductSales().map { rows ->
            rows.map { r ->
                DailyProductSalesUi(
                    label   = "${r.d} · ${r.productName} (${r.quantity}개)",
                    revenue = r.revenue
                )
            }
        }
}

data class DailyProductSalesUi(
    val label: String,
    val revenue: Double
)
