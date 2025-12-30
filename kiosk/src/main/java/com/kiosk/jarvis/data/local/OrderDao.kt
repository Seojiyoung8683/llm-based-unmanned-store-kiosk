package com.kiosk.jarvis.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface OrderDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(order: OrderEntity): Long

    @Query("SELECT * FROM orders ORDER BY orderedAtMillis DESC")
    fun observeAll(): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): OrderEntity?

    @Query("DELETE FROM orders")
    suspend fun deleteAll()

    @Query(
        """
        SELECT 
            strftime(
                '%Y-%m-%d',
                datetime(orderedAtMillis/1000,'unixepoch','localtime')
            ) AS d,
            SUM(totalPrice) AS revenue,
            COUNT(*)        AS orders
        FROM orders
        GROUP BY d
        ORDER BY d
        """
    )
    fun observeDailySales(): Flow<List<DailySalesRow>>
}
