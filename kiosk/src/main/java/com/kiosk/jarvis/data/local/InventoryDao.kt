package com.kiosk.jarvis.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface InventoryDao {
    @Query("SELECT * FROM inventory WHERE productId = :productId LIMIT 1")
    suspend fun get(productId: String): InventoryEntity?

    @Query("SELECT * FROM inventory")
    fun observeAll(): Flow<List<InventoryEntity>>

    @Upsert
    suspend fun upsert(item: InventoryEntity)

    @Query("UPDATE inventory SET stock = stock + :delta, updatedAt = :now WHERE productId = :productId")
    suspend fun addStock(productId: String, delta: Int, now: Long = System.currentTimeMillis())

    @Query("UPDATE inventory SET stock = MAX(stock - :delta, 0), updatedAt = :now WHERE productId = :productId")
    suspend fun decreaseStock(productId: String, delta: Int, now: Long = System.currentTimeMillis())
}
