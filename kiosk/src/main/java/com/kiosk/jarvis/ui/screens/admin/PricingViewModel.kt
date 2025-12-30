package com.kiosk.jarvis.ui.screens.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kiosk.jarvis.model.*
import com.kiosk.jarvis.repository.PricingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PricingUiState(
    val isLoading: Boolean = true,
    val pricingPolicies: List<PricingPolicy> = emptyList(),
    val promotions: List<Promotion> = emptyList(),
    val priceHistory: List<PriceHistory> = emptyList(),
    val selectedTab: Int = 0,
    val error: String? = null,
    val successMessage: String? = null
)

class PricingViewModel : ViewModel() {
    
    private val repository = PricingRepository()
    
    private val _uiState = MutableStateFlow(PricingUiState())
    val uiState: StateFlow<PricingUiState> = _uiState.asStateFlow()
    
    init {
        loadPricingData()
    }
    
    fun loadPricingData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                launch {
                    repository.getAllPricingPolicies().collect { policies ->
                        _uiState.value = _uiState.value.copy(pricingPolicies = policies)
                    }
                }
                
                launch {
                    repository.getAllPromotions().collect { promotions ->
                        _uiState.value = _uiState.value.copy(promotions = promotions)
                    }
                }
                
                launch {
                    repository.getPriceHistory().collect { history ->
                        _uiState.value = _uiState.value.copy(priceHistory = history)
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
    
    fun togglePromotion(promotionId: String, isActive: Boolean) {
        viewModelScope.launch {
            repository.togglePromotion(promotionId, isActive).fold(
                onSuccess = { message ->
                    _uiState.value = _uiState.value.copy(successMessage = message)
                    loadPricingData()
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
