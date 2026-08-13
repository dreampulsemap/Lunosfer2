package io.lunosfer.dreamap.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.lunosfer.dreamap.data.model.SummaryData
import io.lunosfer.dreamap.data.repository.SummaryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class SummaryUiState {
    object Idle : SummaryUiState()
    object Loading : SummaryUiState()
    data class Success(val summary: SummaryData?) : SummaryUiState()
    data class Error(val message: String) : SummaryUiState()
}

class SummaryViewModel(
    private val repository: SummaryRepository = SummaryRepository()
) : ViewModel() {

    private val _weeklySummaryState = MutableStateFlow<SummaryUiState>(SummaryUiState.Idle)
    val weeklySummaryState: StateFlow<SummaryUiState> = _weeklySummaryState.asStateFlow()

    private val _monthlySummaryState = MutableStateFlow<SummaryUiState>(SummaryUiState.Idle)
    val monthlySummaryState: StateFlow<SummaryUiState> = _monthlySummaryState.asStateFlow()

    private val _isGenerating = MutableStateFlow<Boolean>(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    init {
        loadSummaries()
    }

    fun loadSummaries() {
        loadLatest("weekly")
        loadLatest("monthly")
    }

    fun loadLatest(periodType: String) {
        if (periodType == "weekly") _weeklySummaryState.value = SummaryUiState.Loading
        else _monthlySummaryState.value = SummaryUiState.Loading

        viewModelScope.launch {
            repository.getLatestSummary(periodType)
                .onSuccess { summary ->
                    if (periodType == "weekly") _weeklySummaryState.value = SummaryUiState.Success(summary)
                    else _monthlySummaryState.value = SummaryUiState.Success(summary)
                }
                .onFailure { err ->
                    val errorMsg = err.message ?: "Özet yüklenemedi"
                    if (periodType == "weekly") _weeklySummaryState.value = SummaryUiState.Error(errorMsg)
                    else _monthlySummaryState.value = SummaryUiState.Error(errorMsg)
                }
        }
    }

    fun generateSummary(periodType: String) {
        _isGenerating.value = true
        if (periodType == "weekly") _weeklySummaryState.value = SummaryUiState.Loading
        else _monthlySummaryState.value = SummaryUiState.Loading

        viewModelScope.launch {
            repository.generateSummary(periodType)
                .onSuccess { summary ->
                    _isGenerating.value = false
                    if (periodType == "weekly") _weeklySummaryState.value = SummaryUiState.Success(summary)
                    else _monthlySummaryState.value = SummaryUiState.Success(summary)
                }
                .onFailure { err ->
                    _isGenerating.value = false
                    val errorMsg = err.message ?: "Özet oluşturulamadı"
                    if (periodType == "weekly") _weeklySummaryState.value = SummaryUiState.Error(errorMsg)
                    else _monthlySummaryState.value = SummaryUiState.Error(errorMsg)
                }
        }
    }
}
