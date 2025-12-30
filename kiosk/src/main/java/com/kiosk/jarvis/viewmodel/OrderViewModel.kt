package com.kiosk.jarvis.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kiosk.jarvis.data.local.JarvisDatabase
import com.kiosk.jarvis.data.local.OrderEntity
import com.kiosk.jarvis.model.OrderItem
import com.kiosk.jarvis.repository.OrderRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class OrderViewModel(app: Application) : AndroidViewModel(app) {

    // 로컬 DB & 리포지토리
    private val db = JarvisDatabase.get(app)
    private val repo = OrderRepository(db.orderDao())

    // 주문 목록 스트림
    val orders = repo.observeOrders()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /**
     * 주문 생성 + order_items 저장 + 재고 차감까지 처리
     */
    fun placeOrder(
        items: List<OrderItem>,
        paymentMethod: String = "카드",
        status: String = "결제 완료"
    ) {
        val total = items.sumOf { it.lineTotal }
        val now = System.currentTimeMillis()
        val orderNumber = makeOrderNumber(now)

        val entity = OrderEntity(
            orderNumber = orderNumber,
            orderedAtMillis = now,
            totalPrice = total,
            paymentMethod = paymentMethod,
            status = status,
            items = items
        )

        viewModelScope.launch {
            val context = getApplication<Application>()
            repo.insertOrderWithItems(context, entity)
        }
    }

    fun clearAll() {
        viewModelScope.launch { repo.clear() }
    }

    suspend fun getOrder(id: Long) = repo.getOrder(id)

    private fun makeOrderNumber(millis: Long): String {
        val fmt = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.getDefault())
        return "ORD-${fmt.format(Date(millis))}"
    }
}
