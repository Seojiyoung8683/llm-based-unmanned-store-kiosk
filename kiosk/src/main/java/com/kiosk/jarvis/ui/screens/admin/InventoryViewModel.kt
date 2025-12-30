// app/src/main/java/com/kiosk/jarvis/ui/screens/admin/InventoryViewModel.kt
package com.kiosk.jarvis.ui.screens.admin

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kiosk.jarvis.model.InventoryItem
import com.kiosk.jarvis.model.InventoryStatus
import com.kiosk.jarvis.model.RefillTask
import com.kiosk.jarvis.model.StockMovement
import com.kiosk.jarvis.model.VendorDelivery
import com.kiosk.jarvis.model.ProductCategory
import com.kiosk.jarvis.repository.InventoryRepository
import com.kiosk.jarvis.repository.ProductRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect

data class InventoryUiState(
    val isLoading: Boolean = true,
    val inventory: List<InventoryItem> = emptyList(),
    val refillTasks: List<RefillTask> = emptyList(),
    val stockMovements: List<StockMovement> = emptyList(),
    val deliveries: List<VendorDelivery> = emptyList(),
    val selectedTab: Int = 0,
    val error: String? = null,
    val successMessage: String? = null
)

class InventoryViewModel : ViewModel() {

    // ✅ 리필 작업 / 입고 예정 등은 기존 InventoryRepository 그대로 사용
    private val repository = InventoryRepository()

    private val _uiState = MutableStateFlow(InventoryUiState())
    val uiState: StateFlow<InventoryUiState> = _uiState.asStateFlow()

    /**
     * ▶ Context 를 받아서
     *   - ProductRepository 기반으로 재고 현황(InventoryItem) 구성
     *   - InventoryRepository 로 리필 작업 / 입고 예정 등은 그대로 유지
     */
    fun loadInventoryData(context: Context) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                // 🔹 1) Product 테이블 비어 있으면 10개 상품 시드
                ProductRepository.ensureSeeded(context)

                // 🔹 2) ProductRepository 기준으로 재고 현황 구성
                launch {
                    ProductRepository
                        .observeInventory(context)   // Flow<List<ProductInventory>>
                        .collect { productInventoryList ->

                            // 필요에 따라 최소/최대 재고는 상수로 임시 설정
                            val minDefault = 5
                            val maxDefault = 50

                            val inventoryItems = productInventoryList.map { inv ->
                                val stock = inv.stock
                                val status = when {
                                    stock <= 0        -> InventoryStatus.OUT_OF_STOCK
                                    stock <= minDefault -> InventoryStatus.LOW_STOCK
                                    stock >= maxDefault -> InventoryStatus.NORMAL
                                    else              -> InventoryStatus.NORMAL
                                }

                                InventoryItem(
                                    inventoryId   = "INV-${inv.product.id}",        // 또는 그냥 inv.product.id 써도 됨
                                    productId     = inv.product.id,
                                    productName   = inv.product.nameKo,
                                    storeId       = "STORE-001",                    // 매장 하나면 고정값으로 둬도 됨
                                    storeName     = "무인매장 1호점",
                                    currentStock  = stock,
                                    minThreshold  = minDefault,
                                    maxCapacity   = maxDefault,
                                    status        = status,
                                    lastRefillDate = System.currentTimeMillis()     // 마지막 리필 시각 (임시)
                                )
                            }


                            _uiState.value = _uiState.value.copy(
                                inventory = inventoryItems
                            )
                        }
                }

                // 🔹 3) 나머지 데이터(리필 작업 / 재고 이동 / 입고 예정)는 기존 Repository 사용
                launch {
                    repository.getRefillTasks().collect { tasks ->
                        _uiState.value = _uiState.value.copy(refillTasks = tasks)
                    }
                }
                launch {
                    repository.getStockMovements().collect { movements ->
                        _uiState.value = _uiState.value.copy(stockMovements = movements)
                    }
                }
                launch {
                    repository.getVendorDeliveries().collect { deliveries ->
                        _uiState.value = _uiState.value.copy(deliveries = deliveries)
                    }
                }

                _uiState.value = _uiState.value.copy(isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "데이터 로드 실패: ${e.message}"
                )
            }
        }
    }

    fun selectTab(index: Int) {
        _uiState.value = _uiState.value.copy(selectedTab = index)
    }

    fun completeRefillTask(context: Context, taskId: String) {
        viewModelScope.launch {
            repository.completeRefillTask(taskId).fold(
                onSuccess = { message ->
                    _uiState.value = _uiState.value.copy(successMessage = message)
                    // 완료 후 재로드
                    loadInventoryData(context)
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(error = error.message)
                }
            )
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(error = null, successMessage = null)
    }
}
