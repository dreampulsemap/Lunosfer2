package io.lunosfer.dreamap.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.lunosfer.dreamap.data.model.DiaryEntry
import io.lunosfer.dreamap.data.model.UserProfile
import io.lunosfer.dreamap.data.repository.DiaryRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class DiaryStoryViewerUiState {
    object Loading : DiaryStoryViewerUiState()
    data class Content(
        val owner: UserProfile?,
        val entries: List<DiaryEntry>,
        val currentIndex: Int = 0,
        val isSelf: Boolean = false,
        val progress: Float = 0f,
        val isPaused: Boolean = false,
        val actionError: String? = null
    ) : DiaryStoryViewerUiState() {
        val currentEntry: DiaryEntry? get() = entries.getOrNull(currentIndex)
    }
    object Closed : DiaryStoryViewerUiState()
    data class Error(val message: String) : DiaryStoryViewerUiState()
}

class DiaryStoryViewerViewModel(
    private val userId: String,
    private val repository: DiaryRepository = DiaryRepository()
) : ViewModel() {

    private val _state = MutableStateFlow<DiaryStoryViewerUiState>(DiaryStoryViewerUiState.Loading)
    val state: StateFlow<DiaryStoryViewerUiState> = _state.asStateFlow()

    private var timerJob: Job? = null

    init {
        loadData()
        markSeen()
    }

    fun loadData() {
        viewModelScope.launch {
            _state.value = DiaryStoryViewerUiState.Loading
            repository.getEntriesForUser(userId).onSuccess { res ->
                if (res.entries.isEmpty()) {
                    _state.value = DiaryStoryViewerUiState.Error(io.lunosfer.dreamap.DreamapApp.instance.getString(io.lunosfer.dreamap.R.string.error_diary_not_found))
                } else {
                    _state.value = DiaryStoryViewerUiState.Content(
                        owner = res.owner,
                        entries = res.entries,
                        currentIndex = 0,
                        isSelf = res.isSelf
                    )
                    startTimer()
                }
            }.onFailure { err ->
                _state.value = DiaryStoryViewerUiState.Error(err.message ?: io.lunosfer.dreamap.DreamapApp.instance.getString(io.lunosfer.dreamap.R.string.error_stories_load))
            }
        }
    }

    private fun markSeen() {
        viewModelScope.launch {
            repository.markSeen(userId)
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            val durationMs = 4000L
            val stepMs = 50L
            var elapsed = 0L

            while (elapsed < durationMs) {
                delay(stepMs)
                val current = _state.value as? DiaryStoryViewerUiState.Content ?: break
                if (current.isPaused) continue

                elapsed += stepMs
                val p = (elapsed.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
                _state.value = current.copy(progress = p)
            }

            // Time finished -> go next
            nextStory()
        }
    }

    fun pauseTimer() {
        val current = _state.value as? DiaryStoryViewerUiState.Content ?: return
        _state.value = current.copy(isPaused = true)
    }

    fun resumeTimer() {
        val current = _state.value as? DiaryStoryViewerUiState.Content ?: return
        _state.value = current.copy(isPaused = false)
    }

    fun nextStory() {
        val current = _state.value as? DiaryStoryViewerUiState.Content ?: return
        if (current.currentIndex < current.entries.size - 1) {
            _state.value = current.copy(
                currentIndex = current.currentIndex + 1,
                progress = 0f
            )
            startTimer()
        } else {
            // End of stories -> close
            _state.value = DiaryStoryViewerUiState.Closed
        }
    }

    fun previousStory() {
        val current = _state.value as? DiaryStoryViewerUiState.Content ?: return
        if (current.currentIndex > 0) {
            _state.value = current.copy(
                currentIndex = current.currentIndex - 1,
                progress = 0f
            )
            startTimer()
        } else {
            _state.value = current.copy(progress = 0f)
            startTimer()
        }
    }

    fun deleteCurrentEntry() {
        val current = _state.value as? DiaryStoryViewerUiState.Content ?: return
        val entryToDelete = current.currentEntry ?: return

        timerJob?.cancel()
        viewModelScope.launch {
            repository.deleteEntry(entryToDelete.id).onSuccess {
                val updatedList = current.entries.filter { it.id != entryToDelete.id }
                if (updatedList.isEmpty()) {
                    _state.value = DiaryStoryViewerUiState.Closed
                } else {
                    val nextIdx = current.currentIndex.coerceAtMost(updatedList.size - 1)
                    _state.value = current.copy(
                        entries = updatedList,
                        currentIndex = nextIdx,
                        progress = 0f
                    )
                    startTimer()
                }
            }.onFailure { err ->
                _state.value = current.copy(actionError = err.message ?: io.lunosfer.dreamap.DreamapApp.instance.getString(io.lunosfer.dreamap.R.string.error_delete_failed))
                startTimer()
            }
        }
    }

    class Factory(private val userId: String) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DiaryStoryViewerViewModel(userId) as T
        }
    }
}
