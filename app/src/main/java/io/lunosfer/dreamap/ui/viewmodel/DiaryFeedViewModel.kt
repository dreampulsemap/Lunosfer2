package io.lunosfer.dreamap.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.lunosfer.dreamap.data.model.DiaryRing
import io.lunosfer.dreamap.data.repository.DiaryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class DiaryFeedUiState {
    object Loading : DiaryFeedUiState()
    data class Success(val rings: List<DiaryRing>) : DiaryFeedUiState()
    data class Error(val message: String) : DiaryFeedUiState()
}

class DiaryFeedViewModel(
    private val repository: DiaryRepository = DiaryRepository()
) : ViewModel() {

    private val _state = MutableStateFlow<DiaryFeedUiState>(DiaryFeedUiState.Loading)
    val state: StateFlow<DiaryFeedUiState> = _state.asStateFlow()

    init {
        loadFeed()
    }

    fun loadFeed() {
        viewModelScope.launch {
            _state.value = DiaryFeedUiState.Loading
            repository.getFeed().onSuccess { rings ->
                _state.value = DiaryFeedUiState.Success(rings)
            }.onFailure { err ->
                _state.value = DiaryFeedUiState.Error(err.message ?: io.lunosfer.dreamap.DreamapApp.instance.getString(io.lunosfer.dreamap.R.string.error_diary_feed_load))
            }
        }
    }
}
