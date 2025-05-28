package com.mentra.telemetry.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mentra.telemetry.TelemetryService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant

data class MainScreenState(
    val isMonitoring: Boolean = false,
    val recentEvents: List<UsageEvent> = emptyList()
)

data class UsageEvent(
    val appName: String,
    val packageName: String,
    val duration: Duration,
    val timestamp: Instant
)

class MainViewModel(
    private val context: android.content.Context
) : ViewModel() {
    
    private val _state = MutableStateFlow(MainScreenState())
    val state: StateFlow<MainScreenState> = _state.asStateFlow()
    
    init {
        // Start collecting usage events
        viewModelScope.launch {
            TelemetryService.usageEvents.collect { event ->
                _state.update { currentState ->
                    val updatedEvents = (currentState.recentEvents + event)
                        .sortedByDescending { it.timestamp }
                        .take(10) // Keep only the 10 most recent events
                    currentState.copy(recentEvents = updatedEvents)
                }
            }
        }
    }
    
    fun toggleMonitoring() {
        _state.update { currentState ->
            if (currentState.isMonitoring) {
                // Stop monitoring
                context.stopService(TelemetryService.createIntent(context))
                currentState.copy(isMonitoring = false)
            } else {
                // Start monitoring
                TelemetryService.start(context)
                currentState.copy(isMonitoring = true)
            }
        }
    }
    
    // TODO: Add methods to receive and process usage events from TelemetryService
} 