package com.kiosk.jarvis.ui.screens.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kiosk.jarvis.model.Alert
import com.kiosk.jarvis.model.Device
import com.kiosk.jarvis.model.RemoteControlAction
import com.kiosk.jarvis.model.StoreSchedule
import com.kiosk.jarvis.repository.FleetRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class FleetUiState(
    val isLoading: Boolean = true,
    val devices: List<Device> = emptyList(),
    val schedules: List<StoreSchedule> = emptyList(),
    val alerts: List<Alert> = emptyList(),
    val selectedTab: Int = 0,
    val error: String? = null,
    val successMessage: String? = null
)

class FleetViewModel : ViewModel() {
    
    private val repository = FleetRepository()
    
    private val _uiState = MutableStateFlow(FleetUiState())
    val uiState: StateFlow<FleetUiState> = _uiState.asStateFlow()
    
    init {
        loadFleetData()
    }
    
    fun loadFleetData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                launch {
                    repository.getAllDevices().collect { devices ->
                        _uiState.value = _uiState.value.copy(devices = devices)
                    }
                }
                
                launch {
                    repository.getStoreSchedules().collect { schedules ->
                        _uiState.value = _uiState.value.copy(schedules = schedules)
                    }
                }
                
                launch {
                    repository.getAlerts().collect { alerts ->
                        _uiState.value = _uiState.value.copy(alerts = alerts)
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
    
    fun executeControl(action: RemoteControlAction) {
        viewModelScope.launch {
            repository.executeRemoteControl(action).fold(
                onSuccess = { message ->
                    _uiState.value = _uiState.value.copy(successMessage = message)
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(error = error.message)
                }
            )
        }
    }
    
    fun resolveAlert(alertId: String) {
        viewModelScope.launch {
            repository.resolveAlert(alertId).fold(
                onSuccess = {
                    loadFleetData()
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
