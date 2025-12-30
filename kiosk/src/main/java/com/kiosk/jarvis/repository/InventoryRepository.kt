// app/src/main/java/com/kiosk/jarvis/repository/InventoryRepository.kt
package com.kiosk.jarvis.repository

import android.content.Context
import android.database.Cursor
import android.net.Uri
import com.kiosk.jarvis.data.local.InventoryEntity
import com.kiosk.jarvis.data.local.JarvisDatabase
import com.kiosk.jarvis.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext

class InventoryRepository {

    private fun inventoryDao(context: Context) =
        JarvisDatabase.get(context).inventoryDao()

    private fun productDao(context: Context) =
        JarvisDatabase.get(context).productDao()

    fun getAllInventory(context: Context): Flow<List<InventoryItem>> =
        inventoryDao(context).observeAll()
            .combine(productDao(context).observeAll()) { invList, productList ->
                val productMap = productList.associateBy { it.id }

                invList.map { inv ->
                    val p = productMap[inv.productId]
                    val productName = p?.name ?: "(알 수 없는 상품)"

                    val storeId = "S001"
                    val storeName = "무인매장"

                    val currentStock = inv.stock
                    val minThreshold = inv.minThreshold
                    val maxCapacity = 100

                    val status = when {
                        currentStock <= 0 -> InventoryStatus.OUT_OF_STOCK
                        currentStock < minThreshold -> InventoryStatus.LOW_STOCK
                        else -> InventoryStatus.NORMAL
                    }

                    InventoryItem(
                        inventoryId    = inv.productId,
                        productId      = inv.productId,
                        productName    = productName,
                        storeId        = storeId,
                        storeName      = storeName,
                        currentStock   = currentStock,
                        minThreshold   = minThreshold,
                        maxCapacity    = maxCapacity,
                        lastRefillDate = inv.updatedAt,
                        expiryDate     = null,
                        status         = status
                    )
                }
            }
            .flowOn(Dispatchers.IO)

    suspend fun ensureSeeded(context: Context) = withContext(Dispatchers.IO) {
        val invDao = inventoryDao(context)
        val prodDao = productDao(context)

        val allProducts = prodDao.getAllOnce()
        allProducts.forEach { p ->
            val existing = invDao.get(p.id)
            if (existing == null) {
                invDao.upsert(
                    InventoryEntity(
                        productId    = p.id,
                        stock        = 0,
                        minThreshold = 10,
                        location     = null
                    )
                )
            }
        }
    }

    fun getInventoryByStore(context: Context, storeId: String): Flow<List<InventoryItem>> =
        getAllInventory(context).map { list -> list.filter { it.storeId == storeId } }

    fun getRefillTasks(): Flow<List<RefillTask>> = flow {
        delay(500)
        val now = System.currentTimeMillis()
        emit(
            listOf(
                RefillTask(
                    "R001", "S001", "강남점", "P003", "꼬북칩",
                    0, 30, RefillPriority.URGENT, displayPosition = "B-1",
                    status = RefillStatus.PENDING, createdAt = now - 3_600_000
                ),
                RefillTask(
                    "R002", "S001", "강남점", "P002", "새우깡",
                    12, 40, RefillPriority.HIGH, displayPosition = "A-2",
                    status = RefillStatus.PENDING, createdAt = now - 1_800_000
                ),
                RefillTask(
                    "R003", "S002", "홍대점", "P005", "초코파이",
                    8, 25, RefillPriority.MEDIUM, displayPosition = "D-1",
                    status = RefillStatus.IN_PROGRESS, createdAt = now - 7_200_000
                ),
                RefillTask(
                    "R004", "S003", "판교점", "P004", "빼빼로",
                    25, 50, RefillPriority.LOW, displayPosition = "C-1",
                    status = RefillStatus.COMPLETED,
                    createdAt = now - 86_400_000, completedAt = now - 82_800_000
                )
            )
        )
    }

    fun getStockMovements(storeId: String? = null): Flow<List<StockMovement>> = flow {
        delay(500)
        val now = System.currentTimeMillis()
        val list = listOf(
            StockMovement(
                "M001", "S001", "P001", "홈런볼",
                MovementType.REFILL, 50, "정기 리필",
                now - 86_400_000, "관리자"
            ),
            StockMovement(
                "M002", "S001", "P002", "새우깡",
                MovementType.EXPIRED, -5, "유통기한 만료",
                now - 43_200_000, "시스템"
            ),
            StockMovement(
                "M003", "S002", "P004", "빼빼로",
                MovementType.INBOUND, 100, "신규 입고",
                now - 172_800_000, "관리자"
            ),
            StockMovement(
                "M004", "S002", "P003", "꼬북칩",
                MovementType.ADJUSTMENT, -3, "재고 조정",
                now - 259_200_000, "관리자"
            )
        )
        emit(if (storeId != null) list.filter { it.storeId == storeId } else list)
    }

    fun getVendorDeliveries(): Flow<List<VendorDelivery>> = flow {
        delay(500)
        val now = System.currentTimeMillis()
        emit(
            listOf(
                VendorDelivery(
                    deliveryId = "D001",
                    vendorName = "식품 공급사",
                    storeId = "S001",
                    storeName = "강남점",
                    expectedDate = now + 86_400_000,
                    items = listOf(
                        DeliveryItem("P001", "홈런볼", 100),
                        DeliveryItem("P002", "새우깡", 100)
                    ),
                    status = DeliveryStatus.SCHEDULED
                )
            )
        )
    }

    suspend fun completeRefillTask(taskId: String): Result<String> {
        delay(500)
        return Result.success("리필 작업이 완료되었습니다. ($taskId)")
    }

    suspend fun adjustStock(inventoryId: String, adjustment: Int, reason: String): Result<String> {
        delay(500)
        return Result.success("재고가 조정되었습니다. ($inventoryId, $adjustment, $reason)")
    }

    private val PRODUCTS_URI: Uri = Uri.parse("content://com.kiosk.jarvis.inventory/products")

    fun getAllInventoryFromKiosk(context: Context): Flow<List<InventoryItem>> = flow {
        val cr = context.contentResolver
        val result = mutableListOf<InventoryItem>()
        cr.query(PRODUCTS_URI, null, null, null, null)?.use { c ->
            val idxId        = c.getCol("id")
            val idxName      = c.getCol("name")
            val idxUpdatedAt = c.getCol("updatedAt")
            val idxDeleted   = c.getCol("deleted")

            while (c.moveToNext()) {
                val productId = c.getStringOrNull(idxId) ?: continue
                val name      = c.getStringOrNull(idxName) ?: ""
                val deleted   = c.getIntOrZero(idxDeleted) != 0

                val storeId      = "S001"
                val storeName    = "매장1"
                val currentStock = 0
                val minThreshold = 20
                val maxCapacity  = 100
                val lastRefill   = c.getLongOrNull(idxUpdatedAt) ?: 0L
                val expiryDate: Long? = null

                val status = when {
                    deleted -> InventoryStatus.OUT_OF_STOCK
                    currentStock <= 0 -> InventoryStatus.OUT_OF_STOCK
                    currentStock < minThreshold -> InventoryStatus.LOW_STOCK
                    else -> InventoryStatus.NORMAL
                }

                result.add(
                    InventoryItem(
                        inventoryId    = "INV-$productId",
                        productId      = productId,
                        productName    = name,
                        storeId        = storeId,
                        storeName      = storeName,
                        currentStock   = currentStock,
                        minThreshold   = minThreshold,
                        maxCapacity    = maxCapacity,
                        lastRefillDate = lastRefill,
                        expiryDate     = expiryDate,
                        status         = status
                    )
                )
            }
        }
        emit(result)
    }.flowOn(Dispatchers.IO)

    private fun Cursor.getCol(name: String): Int =
        getColumnIndex(name).takeIf { it >= 0 } ?: -1

    private fun Cursor.getStringOrNull(index: Int): String? =
        if (index >= 0 && !isNull(index)) getString(index) else null

    private fun Cursor.getIntOrZero(index: Int): Int =
        if (index >= 0 && !isNull(index)) getInt(index) else 0

    private fun Cursor.getLongOrNull(index: Int): Long? =
        if (index >= 0 && !isNull(index)) getLong(index) else null
}
