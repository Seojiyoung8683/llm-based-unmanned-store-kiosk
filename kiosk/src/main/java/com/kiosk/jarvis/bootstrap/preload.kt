// com/kiosk/jarvis/bootstrap/preload.kt
package com.kiosk.jarvis.bootstrap

import com.kiosk.jarvis.data.local.JarvisDatabase
import com.kiosk.jarvis.model.ProductData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Log

suspend fun preloadProductsAndInventory(db: JarvisDatabase) = withContext(Dispatchers.IO) {
    val uiList = ProductData.products

    Log.d("PRELOAD", "preloading ${uiList.size} products")

    val productEntities = uiList.map { it.toProductEntity() }
    val inventoryEntities = uiList.map { it.toInventoryEntity(defaultStock = 0) }

    db.productDao().upsertAll(productEntities)
    inventoryEntities.forEach { entity ->
        db.inventoryDao().upsert(entity)
    }

    Log.d("PRELOAD", "done products=${productEntities.size}, inventory=${inventoryEntities.size}")
}
