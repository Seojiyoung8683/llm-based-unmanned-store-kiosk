// app/src/main/java/com/kiosk/jarvis/data/InventoryManager.kt
package com.kiosk.jarvis.data

import android.content.Context
import com.kiosk.jarvis.data.local.JarvisDatabase
import com.kiosk.jarvis.data.local.InventoryEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

object InventoryManager {
    private lateinit var db: JarvisDatabase
    fun init(context: Context) {
        db = JarvisDatabase.get(context)
    }

    fun getStock(productId: String): Int = runBlocking(Dispatchers.IO) {
        val row = db.inventoryDao().get(productId)
        row?.stock ?: 0
    }

    fun setStock(productId: String, quantity: Int) = runBlocking(Dispatchers.IO) {
        val safe = quantity.coerceAtLeast(0)
        val now = System.currentTimeMillis()
        val curr = db.inventoryDao().get(productId)
        if (curr == null) {
            db.inventoryDao().upsert(
                InventoryEntity(
                    productId = productId,
                    stock = safe,
                    updatedAt = now
                )
            )
        } else {
            db.inventoryDao().upsert(curr.copy(stock = safe, updatedAt = now))
        }
    }

    fun decrement(productId: String, quantity: Int) = runBlocking(Dispatchers.IO) {
        db.inventoryDao().decreaseStock(productId, quantity)
    }

    fun bulkDecrement(items: List<com.kiosk.jarvis.model.CartItem>) = runBlocking(Dispatchers.IO) {
        items.forEach { item ->
            val pid = item.product.id
            db.inventoryDao().decreaseStock(pid, item.quantity)
        }
    }
}
