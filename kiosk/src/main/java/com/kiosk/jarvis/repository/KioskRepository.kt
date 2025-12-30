package com.kiosk.jarvis.data.repo

import com.kiosk.jarvis.data.local.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class KioskRepository(
    private val productDao: ProductDao,
    private val inventoryDao: InventoryDao
) {
    fun observeProducts(): Flow<List<ProductEntity>> = productDao.observeAll()
    fun observeInventory(): Flow<List<InventoryEntity>> = inventoryDao.observeAll()

    suspend fun scanBarcode(barcode: String) =
        withContext(Dispatchers.IO) { productDao.getByBarcode(barcode) }

    suspend fun decreaseStock(productId: String, qty: Int) =
        withContext(Dispatchers.IO) { inventoryDao.decreaseStock(productId, qty) }

    suspend fun upsertProducts(items: List<ProductEntity>) =
        withContext(Dispatchers.IO) { productDao.upsertAll(items) }

    suspend fun upsertInventory(item: InventoryEntity) =
        withContext(Dispatchers.IO) { inventoryDao.upsert(item) }
}
