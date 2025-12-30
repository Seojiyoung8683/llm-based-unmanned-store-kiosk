package com.kiosk.jarvis.viewmodel

import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import com.kiosk.jarvis.model.CartItem
import com.kiosk.jarvis.model.Product

class CartViewModel : ViewModel() {

    // 장바구니 아이템 목록
    private val _cartItems = mutableStateListOf<CartItem>()
    val cartItems: List<CartItem> get() = _cartItems

    // 총 금액
    val totalPrice: Int
        get() = _cartItems.sumOf { it.product.price * it.quantity }

    // 총 수량
    val totalItems: Int
        get() = _cartItems.sumOf { it.quantity }

    /**
     * 화면에서 넘겨준 maxStock 기준으로 재고 체크하며 담기
     */
    fun addToCart(product: Product, maxStock: Int) {
        val existing = _cartItems.find { it.product.id == product.id }
        val currentQty = existing?.quantity ?: 0

        Log.d(
            "CartDebug",
            "addToCart 진입: id=${product.id}, maxStock=$maxStock, currentQty=$currentQty, cartSize=${_cartItems.size}"
        )

        if (currentQty >= maxStock) {
            Log.d("CartDebug", "재고 한도 도달 → 더 이상 추가 안함 (currentQty=$currentQty, maxStock=$maxStock)")
            return
        }

        if (existing != null) {
            val idx = _cartItems.indexOf(existing)
            _cartItems[idx] = existing.copy(quantity = existing.quantity + 1)
            Log.d("CartDebug", "기존 항목 수량 +1 → ${_cartItems[idx]}")
        } else {
            _cartItems.add(CartItem(product, 1))
            Log.d("CartDebug", "새 항목 추가 → ${_cartItems.last()}")
        }
    }

    /**
     * 기존 호출 코드들 깨지지 않게 오버로드 남겨두기 (재고 제한 없이 담기)
     */
    fun addToCart(product: Product) {
        addToCart(product, Int.MAX_VALUE)
    }

    /**
     * 상품 1개 수량 줄이기 (0 되면 제거)
     */
    fun removeFromCart(product: Product) {
        val existing = _cartItems.find { it.product.id == product.id } ?: return
        if (existing.quantity > 1) {
            val idx = _cartItems.indexOf(existing)
            _cartItems[idx] = existing.copy(quantity = existing.quantity - 1)
        } else {
            _cartItems.remove(existing)
        }
    }

    /**
     * 해당 상품을 장바구니에서 통째로 제거 (삭제 버튼용)
     */
    fun removeItem(product: Product) {
        val existing = _cartItems.find { it.product.id == product.id } ?: return
        _cartItems.remove(existing)
    }

    /**
     * 장바구니 비우기 (전체 삭제 버튼용)
     */
    fun clearCart() {
        _cartItems.clear()
    }
}
