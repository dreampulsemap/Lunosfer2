package io.lunosfer.dreamap.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.lunosfer.dreamap.data.model.MentalWallResponse
import io.lunosfer.dreamap.data.model.ProphetResponse
import io.lunosfer.dreamap.data.model.PsycheMapResponse
import io.lunosfer.dreamap.data.repository.SpiritualToolsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class MentalWallUiState {
    object Idle : MentalWallUiState()
    object Loading : MentalWallUiState()
    data class Success(val response: MentalWallResponse) : MentalWallUiState()
    data class Error(val message: String) : MentalWallUiState()
}

sealed class PsycheMapUiState {
    object Idle : PsycheMapUiState()
    object Loading : PsycheMapUiState()
    data class Success(val response: PsycheMapResponse) : PsycheMapUiState()
    data class Error(val message: String) : PsycheMapUiState()
}

sealed class ProphetUiState {
    object Idle : ProphetUiState()
    object Loading : ProphetUiState()
    data class Success(val response: ProphetResponse) : ProphetUiState()
    data class Error(val message: String) : ProphetUiState()
}

class SpiritualToolsViewModel(
    private val repository: SpiritualToolsRepository = SpiritualToolsRepository()
) : ViewModel() {

    private val _mentalWallState = MutableStateFlow<MentalWallUiState>(MentalWallUiState.Idle)
    val mentalWallState: StateFlow<MentalWallUiState> = _mentalWallState.asStateFlow()

    private val _psycheMapState = MutableStateFlow<PsycheMapUiState>(PsycheMapUiState.Idle)
    val psycheMapState: StateFlow<PsycheMapUiState> = _psycheMapState.asStateFlow()

    private val _prophetState = MutableStateFlow<ProphetUiState>(ProphetUiState.Idle)
    val prophetState: StateFlow<ProphetUiState> = _prophetState.asStateFlow()

    init {
        loadPsycheMap()
    }

    fun generateMentalWall() {
        _mentalWallState.value = MentalWallUiState.Loading
        viewModelScope.launch {
            repository.generateMentalWall(io.lunosfer.dreamap.DreamapApp.instance.getString(io.lunosfer.dreamap.R.string.app_lang_code))
                .onSuccess { res ->
                    _mentalWallState.value = MentalWallUiState.Success(res)
                }
                .onFailure { err ->
                    _mentalWallState.value = MentalWallUiState.Error(err.message ?: io.lunosfer.dreamap.DreamapApp.instance.getString(io.lunosfer.dreamap.R.string.error_mental_wall))
                }
        }
    }

    fun loadPsycheMap() {
        _psycheMapState.value = PsycheMapUiState.Loading
        viewModelScope.launch {
            repository.getPsycheMap()
                .onSuccess { res ->
                    _psycheMapState.value = PsycheMapUiState.Success(res)
                }
                .onFailure { err ->
                    _psycheMapState.value = PsycheMapUiState.Error(err.message ?: io.lunosfer.dreamap.DreamapApp.instance.getString(io.lunosfer.dreamap.R.string.error_psyche_map))
                }
        }
    }

    fun consultProphet(question: String) {
        if (question.isBlank()) return
        _prophetState.value = ProphetUiState.Loading
        viewModelScope.launch {
            repository.consultProphet(question, io.lunosfer.dreamap.DreamapApp.instance.getString(io.lunosfer.dreamap.R.string.app_lang_code))
                .onSuccess { res ->
                    _prophetState.value = ProphetUiState.Success(res)
                }
                .onFailure { err ->
                    _prophetState.value = ProphetUiState.Error(err.message ?: io.lunosfer.dreamap.DreamapApp.instance.getString(io.lunosfer.dreamap.R.string.error_prophet_failed))
                }
        }
    }
}
