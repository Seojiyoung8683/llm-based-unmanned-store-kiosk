// app/src/main/java/com/kiosk/jarvis/ui/screens/admin/ProductsViewModel.kt
package com.kiosk.jarvis.ui.screens.admin

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kiosk.jarvis.model.Product
import com.kiosk.jarvis.model.ProductCategory
import com.kiosk.jarvis.repository.ProductRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class ProductsUiState(
    val isLoading: Boolean = true,
    val products: List<Product> = emptyList(),
    val selectedCategory: ProductCategory? = null,
    val error: String? = null,
    val toast: String? = null
)

class ProductsViewModel(app: Application) : AndroidViewModel(app) {

    private val _ui = MutableStateFlow(ProductsUiState())
    val ui: StateFlow<ProductsUiState> = _ui

    init { load() }

    fun load() {
        viewModelScope.launch {
            _ui.value = _ui.value.copy(isLoading = true, error = null)
            try {
                // ✅ 최초 1회 시드
                ProductRepository.ensureSeeded(getApplication())

                // ✅ 로컬 DB 관찰
                ProductRepository.observeAll(getApplication()).collectLatest { list ->
                    val filtered = _ui.value.selectedCategory?.let { cat ->
                        list.filter { it.category == cat }
                    } ?: list
                    _ui.value = _ui.value.copy(isLoading = false, products = filtered)
                }
            } catch (e: Exception) {
                _ui.value = _ui.value.copy(isLoading = false, error = "상품 로드 실패: ${e.message}")
            }
        }
    }

    fun filterBy(category: ProductCategory?) {
        _ui.value = _ui.value.copy(selectedCategory = category)
        // Flow 기반이라 바로 반영되지만, 간단히 reload
        load()
    }

    /** 생성/수정 공통 저장 */
    fun saveProduct(
        existingId: String?,
        nameKo: String,
        nameEn: String,
        category: ProductCategory,
        price: Int,
        location: String,
        description: String,
        barcode: String?
    ) {
        viewModelScope.launch {
            try {
                val id = existingId ?: generateProductId()
                val product = Product(
                    id = id,
                    nameKo = nameKo,
                    nameEn = nameEn,
                    category = category,
                    price = price,
                    location = location,
                    imageRes = 0,          // DB → Flow에서 다시 계산됨
                    description = description,
                    barcode = barcode
                )

                ProductRepository.upsert(getApplication(), product)
                    .onSuccess {
                        _ui.value = _ui.value.copy(
                            toast = if (existingId == null) "상품이 추가되었습니다." else "상품이 수정되었습니다."
                        )
                    }
                    .onFailure { e ->
                        _ui.value = _ui.value.copy(error = "상품 저장 실패: ${e.message}")
                    }
            } catch (e: Exception) {
                _ui.value = _ui.value.copy(error = "상품 저장 실패: ${e.message}")
            }
        }
    }

    fun delete(productId: String) {
        viewModelScope.launch {
            ProductRepository.delete(getApplication(), productId).onSuccess {
                _ui.value = _ui.value.copy(toast = "삭제 완료")
            }.onFailure { e ->
                _ui.value = _ui.value.copy(error = e.message)
            }
        }
    }

    fun clearToast() { _ui.value = _ui.value.copy(toast = null) }

    /** 새 상품 ID 생성(P + 타임스탬프) */
    private fun generateProductId(): String =
        "P" + System.currentTimeMillis()
}
