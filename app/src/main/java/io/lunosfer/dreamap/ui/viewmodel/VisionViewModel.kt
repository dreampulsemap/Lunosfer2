package io.lunosfer.dreamap.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.lunosfer.dreamap.data.model.DailySeedItem
import io.lunosfer.dreamap.data.model.Goal
import io.lunosfer.dreamap.data.repository.VisionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class CompassUiState {
    object Idle : CompassUiState()
    object Loading : CompassUiState()
    data class Success(val reading: String, val archetype: String?, val color: String?) : CompassUiState()
    object AlreadyUsedToday : CompassUiState()
    data class Error(val message: String) : CompassUiState()
}

class VisionViewModel(
    private val repository: VisionRepository = VisionRepository()
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<List<Goal>>>(UiState.Loading)
    val state: StateFlow<UiState<List<Goal>>> = _state.asStateFlow()

    private val _compassState = MutableStateFlow<CompassUiState>(CompassUiState.Idle)
    val compassState: StateFlow<CompassUiState> = _compassState.asStateFlow()

    private val _dailySeeds = MutableStateFlow<List<DailySeedItem>>(emptyList())
    val dailySeeds: StateFlow<List<DailySeedItem>> = _dailySeeds.asStateFlow()

    private val _seedGeneratingMap = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val seedGeneratingMap: StateFlow<Map<String, Boolean>> = _seedGeneratingMap.asStateFlow()

    init {
        load()
        loadDailySeeds()
    }

    fun retry() {
        load()
        loadDailySeeds()
    }

    private fun load() {
        _state.value = UiState.Loading
        viewModelScope.launch {
            repository.loadFirstPage()
                .onSuccess { _state.value = UiState.Success(it) }
                .onFailure { _state.value = UiState.Error(it.message ?: io.lunosfer.dreamap.DreamapApp.instance.getString(io.lunosfer.dreamap.R.string.error_unknown)) }
        }
    }

    fun fetchDailyCompass() {
        _compassState.value = CompassUiState.Loading
        viewModelScope.launch {
            repository.getDailyCompass("tr")
                .onSuccess { res ->
                    if (res.error == "already_used_today" || (res.ok == false && res.error != null)) {
                        if (res.error == "already_used_today") {
                            _compassState.value = CompassUiState.AlreadyUsedToday
                        } else {
                            _compassState.value = CompassUiState.Error(res.error ?: io.lunosfer.dreamap.DreamapApp.instance.getString(io.lunosfer.dreamap.R.string.error_compass_failed))
                        }
                    } else if (res.data != null) {
                        _compassState.value = CompassUiState.Success(
                            reading = res.data.reading ?: "",
                            archetype = res.data.archetype,
                            color = res.data.color
                        )
                    } else {
                        _compassState.value = CompassUiState.AlreadyUsedToday
                    }
                }
                .onFailure { err ->
                    val msg = err.message ?: ""
                    if (msg.contains("429") || msg.contains("already_used")) {
                        _compassState.value = CompassUiState.AlreadyUsedToday
                    } else {
                        _compassState.value = CompassUiState.Error(io.lunosfer.dreamap.DreamapApp.instance.getString(io.lunosfer.dreamap.R.string.error_daily_compass_failed))
                    }
                }
        }
    }

    fun loadDailySeeds() {
        viewModelScope.launch {
            repository.getDailySeeds()
                .onSuccess { seeds ->
                    _dailySeeds.value = seeds
                }
        }
    }

    fun generateSeedForGoal(goalId: String) {
        val currentMap = _seedGeneratingMap.value.toMutableMap()
        currentMap[goalId] = true
        _seedGeneratingMap.value = currentMap

        viewModelScope.launch {
            repository.generateDailySeed(goalId, "tr")
                .onSuccess { newSeed ->
                    if (newSeed != null) {
                        val currentList = _dailySeeds.value.filterNot { it.goalId == goalId } + newSeed
                        _dailySeeds.value = currentList
                    } else {
                        loadDailySeeds()
                    }
                }
                .onFailure {
                    loadDailySeeds()
                }
            val doneMap = _seedGeneratingMap.value.toMutableMap()
            doneMap[goalId] = false
            _seedGeneratingMap.value = doneMap
        }
    }

    fun toggleSeedCompletion(seed: DailySeedItem) {
        val updatedSeeds = _dailySeeds.value.map { item ->
            if (item.id == seed.id) item.copy(isCompleted = !item.isCompleted) else item
        }
        _dailySeeds.value = updatedSeeds

        viewModelScope.launch {
            repository.completeDailySeed(seed.id)
                .onFailure {
                    loadDailySeeds()
                }
        }
    }
}
