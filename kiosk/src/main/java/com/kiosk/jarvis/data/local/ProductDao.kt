// ProductDao.kt
package com.kiosk.jarvis.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {

    @Query("SELECT * FROM products ORDER BY id")
    fun observeAll(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): ProductEntity?

    @Query("SELECT * FROM products WHERE barcode = :barcode LIMIT 1")
    suspend fun getByBarcode(barcode: String): ProductEntity?

    @Query("SELECT COUNT(*) FROM products")
    suspend fun count(): Int

    @Query("SELECT * FROM products ORDER BY id")
    suspend fun getAllOnce(): List<ProductEntity>

    @Query("DELETE FROM products WHERE id = :id")
    suspend fun hardDelete(id: String)

    @Upsert
    suspend fun upsert(item: ProductEntity)

    @Upsert
    suspend fun upsertAll(items: List<ProductEntity>)

    @Query("UPDATE products SET stock = stock + :delta, updatedAt = :updatedAt WHERE id = :id")
    suspend fun increaseStock(id: String, delta: Int, updatedAt: Long)

    @Query("SELECT stock FROM products WHERE id = :id LIMIT 1")
    suspend fun getStock(id: String): Int?
}
