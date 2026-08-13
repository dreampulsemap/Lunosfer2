package io.lunosfer.dreamap.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.lunosfer.dreamap.data.model.Dream
import io.lunosfer.dreamap.data.model.Goal
import io.lunosfer.dreamap.data.repository.ExploreRepository
import io.lunosfer.dreamap.data.repository.VisionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class ExploreTab {
    DREAMSCAPE,
    VISION,
    VICTORY,
    PHOENIX
}

class ExploreViewModel(
    private val repository: ExploreRepository = ExploreRepository(),
    private val visionRepository: VisionRepository = VisionRepository()
) : ViewModel() {

    private val _activeTab = MutableStateFlow(ExploreTab.DREAMSCAPE)
    val activeTab: StateFlow<ExploreTab> = _activeTab.asStateFlow()

    private val _state = MutableStateFlow<UiState<List<Dream>>>(UiState.Loading)
    val state: StateFlow<UiState<List<Dream>>> = _state.asStateFlow()

    private val _visionState = MutableStateFlow<UiState<List<Goal>>>(UiState.Loading)
    val visionState: StateFlow<UiState<List<Goal>>> = _visionState.asStateFlow()

    private val _victoryState = MutableStateFlow<UiState<List<Goal>>>(UiState.Loading)
    val victoryState: StateFlow<UiState<List<Goal>>> = _victoryState.asStateFlow()

    private val _phoenixState = MutableStateFlow<UiState<List<Goal>>>(UiState.Loading)
    val phoenixState: StateFlow<UiState<List<Goal>>> = _phoenixState.asStateFlow()

    init {
        loadDreams()
    }

    fun selectTab(tab: ExploreTab) {
        _activeTab.value = tab
        when (tab) {
            ExploreTab.DREAMSCAPE -> {
                if (_state.value is UiState.Loading) {
                    loadDreams()
                }
            }
            ExploreTab.VISION -> {
                if (_visionState.value is UiState.Loading) {
                    loadGoals(ExploreTab.VISION, "active")
                }
            }
            ExploreTab.VICTORY -> {
                if (_victoryState.value is UiState.Loading) {
                    loadGoals(ExploreTab.VICTORY, "completed")
                }
            }
            ExploreTab.PHOENIX -> {
                if (_phoenixState.value is UiState.Loading) {
                    loadGoals(ExploreTab.PHOENIX, "abandoned")
                }
            }
        }
    }

    fun retry() {
        retry(_activeTab.value)
    }

    fun retry(tab: ExploreTab) {
        when (tab) {
            ExploreTab.DREAMSCAPE -> loadDreams()
            ExploreTab.VISION -> loadGoals(ExploreTab.VISION, "active")
            ExploreTab.VICTORY -> loadGoals(ExploreTab.VICTORY, "completed")
            ExploreTab.PHOENIX -> loadGoals(ExploreTab.PHOENIX, "abandoned")
        }
    }

    private fun loadDreams() {
        _state.value = UiState.Loading
        viewModelScope.launch {
            repository.loadFirstPage()
                .onSuccess { _state.value = UiState.Success(it) }
                .onFailure { _state.value = UiState.Error(it.message ?: io.lunosfer.dreamap.DreamapApp.instance.getString(io.lunosfer.dreamap.R.string.error_unknown)) }
        }
    }

    private fun loadGoals(tab: ExploreTab, status: String) {
        val stateFlow = when (tab) {
            ExploreTab.VISION -> _visionState
            ExploreTab.VICTORY -> _victoryState
            ExploreTab.PHOENIX -> _phoenixState
            else -> return
        }
        stateFlow.value = UiState.Loading
        viewModelScope.launch {
            visionRepository.loadHubGoals(status)
                .onSuccess { stateFlow.value = UiState.Success(it) }
                .onFailure { stateFlow.value = UiState.Error(it.message ?: io.lunosfer.dreamap.DreamapApp.instance.getString(io.lunosfer.dreamap.R.string.error_unknown)) }
        }
    }
}
