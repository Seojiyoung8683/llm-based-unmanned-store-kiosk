package com.kiosk.jarvis.ui.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kiosk.jarvis.repository.DashboardRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class DashboardUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val salesMetrics: com.kiosk.jarvis.model.dashboard.SalesMetrics? = null,
    val storeSummaries: List<com.kiosk.jarvis.model.dashboard.StoreSummary> = emptyList(),
    val dailySales: List<com.kiosk.jarvis.model.dashboard.SalesData> = emptyList(),
    val topProducts: List<com.kiosk.jarvis.model.dashboard.TopProduct> = emptyList()
)

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = DashboardRepository(application.applicationContext)

    private val refreshTrigger = MutableStateFlow(0)

    val uiState: StateFlow<DashboardUiState> =
        refreshTrigger.flatMapLatest {
            combine(
                repo.getSalesMetrics(),
                repo.getStoreSummaries(),
                repo.getDailySales(days = 7),
                repo.getTopProducts(limit = 5)
            ) { metrics, stores, daily, top ->
                DashboardUiState(
                    isLoading = false,
                    error = null,
                    salesMetrics = metrics,
                    storeSummaries = stores,
                    dailySales = daily,
                    topProducts = top
                )
            }
                .catch { e ->
                    emit(
                        DashboardUiState(
                            isLoading = false,
                            error = e.message ?: "데이터를 불러오지 못했습니다."
                        )
                    )
                }
                .onStart { emit(DashboardUiState(isLoading = true)) }
        }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = DashboardUiState(isLoading = true)
            )

    fun refreshData() {
        viewModelScope.launch { refreshTrigger.update { it + 1 } }
    }
}
