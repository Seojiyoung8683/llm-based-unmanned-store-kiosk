package com.kiosk.jarvis.repository

import android.content.Context
import androidx.room.withTransaction
import com.kiosk.jarvis.data.local.JarvisDatabase
import com.kiosk.jarvis.data.local.OrderDao
import com.kiosk.jarvis.data.local.OrderEntity
import com.kiosk.jarvis.data.local.OrderItemEntity
import kotlinx.coroutines.flow.Flow

class OrderRepository(private val orderDao: OrderDao) {

    private fun db(context: Context) = JarvisDatabase.get(context)


    suspend fun insertOrderWithItems(
        context: Context,
        order: OrderEntity
    ): Long {
        val database = db(context)
        var newId = 0L

        database.withTransaction {
            newId = database.orderDao().insert(order)

            val items = order.items.map { oi ->
                OrderItemEntity(
                    orderId = newId,
                    productId = oi.productId,
                    quantity = oi.quantity,
                    unitPrice = oi.unitPrice,
                    orderedAtMillis = order.orderedAtMillis
                )
            }
            database.orderItemDao().insertAll(items)

            val productDao = database.productDao()
            order.items.forEach { oi ->
                productDao.increaseStock(
                    id = oi.productId,
                    delta = -oi.quantity,
                    updatedAt = order.orderedAtMillis
                )
            }
        }

        return newId
    }

    fun observeOrders(): Flow<List<OrderEntity>> = orderDao.observeAll()
    suspend fun getOrder(id: Long) = orderDao.getById(id)
    suspend fun add(order: OrderEntity) = orderDao.insert(order)
    suspend fun clear() = orderDao.deleteAll()
}
