package io.lunosfer.dreamap.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.lunosfer.dreamap.data.model.Goal
import io.lunosfer.dreamap.data.model.GoalSlide
import io.lunosfer.dreamap.data.model.UserProfile
import io.lunosfer.dreamap.data.repository.VisionRepository
import io.lunosfer.dreamap.supabase.supabaseClient
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * components/SlidesViewer.jsx'in Android karşılığı — "Vizyon Slaytları",
 * oto-oynatan, Instagram/TikTok Stories tarzı tam ekran görüntüleyici.
 *
 * Kapsam bilerek sınırlı tutuldu: web'deki yorum sheet'i, bildir sheet'i ve
 * üç nokta menüsü (Düzenle) bu ilk sürümde YOK — sadece izleme deneyimi +
 * mana ver/kaldır + slayt kaydet + (sahipse) slayt sil. Gerekirse ayrı bir
 * iş olarak eklenebilir.
 */
sealed class SlidesViewerUiState {
    object Loading : SlidesViewerUiState()

    data class Content(
        val goal: Goal?,
        val owner: UserProfile?,
        val slides: List<GoalSlide>,
        val currentIndex: Int = 0,
        val isOwner: Boolean = false,
        val progress: Float = 0f,
        val isPaused: Boolean = false,
        val hasReacted: Boolean = false,
        val believersCount: Int = 0,
        val actionError: String? = null
    ) : SlidesViewerUiState() {
        val currentSlide: GoalSlide? get() = slides.getOrNull(currentIndex)
    }

    object Closed : SlidesViewerUiState()
    data class Error(val message: String) : SlidesViewerUiState()
}

class SlidesViewerViewModel(
    private val goalId: String,
    private val repository: VisionRepository = VisionRepository()
) : ViewModel() {

    private val _state = MutableStateFlow<SlidesViewerUiState>(SlidesViewerUiState.Loading)
    val state: StateFlow<SlidesViewerUiState> = _state.asStateFlow()

    private val currentUserId: String?
        get() = supabaseClient.auth.currentUserOrNull()?.id

    private var timerJob: Job? = null

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _state.value = SlidesViewerUiState.Loading

            val goal = runCatching {
                supabaseClient.postgrest["goals"]
                    .select(Columns.raw("*, user_profiles:user_id(*)")) {
                        filter { eq("id", goalId) }
                    }.decodeSingle<Goal>()
            }.getOrNull()

            repository.loadGoalSlides(goalId).onSuccess { res ->
                if (res.slides.isEmpty()) {
                    _state.value = SlidesViewerUiState.Error(
                        io.lunosfer.dreamap.DreamapApp.instance.getString(io.lunosfer.dreamap.R.string.error_slides_not_found)
                    )
                } else {
                    _state.value = SlidesViewerUiState.Content(
                        goal = goal,
                        owner = res.owner,
                        slides = res.slides,
                        currentIndex = 0,
                        isOwner = currentUserId != null && goal?.userId == currentUserId,
                        hasReacted = goal?.hasReacted ?: false,
                        believersCount = goal?.believersCount ?: 0
                    )
                    startTimer()
                }
            }.onFailure { err ->
                _state.value = SlidesViewerUiState.Error(
                    err.message ?: io.lunosfer.dreamap.DreamapApp.instance.getString(io.lunosfer.dreamap.R.string.error_slides_load)
                )
            }
        }
    }

    // Her slayt kendi duration_seconds'ı kadar ekranda kalır, süre dolunca
    // bir sonrakine geçer (SlidesViewer.jsx ile aynı mantık).
    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            val current0 = _state.value as? SlidesViewerUiState.Content ?: return@launch
            val durationMs = (current0.currentSlide?.durationSeconds ?: 4).coerceAtLeast(1) * 1000L
            val stepMs = 50L
            var elapsed = 0L

            while (elapsed < durationMs) {
                delay(stepMs)
                val current = _state.value as? SlidesViewerUiState.Content ?: return@launch
                if (current.isPaused) continue
                elapsed += stepMs
                val p = (elapsed.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
                _state.value = current.copy(progress = p)
            }
            nextSlide()
        }
    }

    fun pauseTimer() {
        val current = _state.value as? SlidesViewerUiState.Content ?: return
        _state.value = current.copy(isPaused = true)
    }

    fun resumeTimer() {
        val current = _state.value as? SlidesViewerUiState.Content ?: return
        _state.value = current.copy(isPaused = false)
    }

    // Son slayttaysa hiçbir şey yapma — web'deki gibi dur, döngüye girme,
    // otomatik kapatma.
    fun nextSlide() {
        val current = _state.value as? SlidesViewerUiState.Content ?: return
        if (current.currentIndex < current.slides.size - 1) {
            _state.value = current.copy(currentIndex = current.currentIndex + 1, progress = 0f)
            startTimer()
        }
    }

    // İlk slayttaysa hiçbir şey yapma (web'deki gibi).
    fun previousSlide() {
        val current = _state.value as? SlidesViewerUiState.Content ?: return
        if (current.currentIndex > 0) {
            _state.value = current.copy(currentIndex = current.currentIndex - 1, progress = 0f)
            startTimer()
        }
    }

    fun close() {
        timerJob?.cancel()
        _state.value = SlidesViewerUiState.Closed
    }

    fun toggleMana() {
        val current = _state.value as? SlidesViewerUiState.Content ?: return
        if (current.isOwner) return
        val wasReacted = current.hasReacted
        val oldCount = current.believersCount

        if (wasReacted) {
            _state.value = current.copy(hasReacted = false, believersCount = maxOf(0, oldCount - 1))
            viewModelScope.launch {
                repository.removeMana(goalId).onFailure {
                    val latest = _state.value as? SlidesViewerUiState.Content ?: return@onFailure
                    _state.value = latest.copy(hasReacted = wasReacted, believersCount = oldCount)
                }
            }
        } else {
            _state.value = current.copy(hasReacted = true, believersCount = oldCount + 1)
            viewModelScope.launch {
                repository.giveMana(goalId, 1).onFailure { err ->
                    val latest = _state.value as? SlidesViewerUiState.Content ?: return@onFailure
                    _state.value = latest.copy(
                        hasReacted = wasReacted,
                        believersCount = oldCount,
                        actionError = err.message
                    )
                }
            }
        }
    }

    fun toggleSaveSlide() {
        val current = _state.value as? SlidesViewerUiState.Content ?: return
        val slide = current.currentSlide ?: return
        val index = current.currentIndex
        val wasSaved = slide.hasSaved ?: false
        val oldCount = slide.savesCount ?: 0

        val updatedSlide = slide.copy(
            hasSaved = !wasSaved,
            savesCount = if (wasSaved) maxOf(0, oldCount - 1) else oldCount + 1
        )
        _state.value = current.copy(
            slides = current.slides.toMutableList().also { it[index] = updatedSlide }
        )

        viewModelScope.launch {
            repository.toggleSlideSave(slide.id).onFailure {
                val latest = _state.value as? SlidesViewerUiState.Content ?: return@onFailure
                _state.value = latest.copy(
                    slides = latest.slides.toMutableList().also { it[index] = slide }
                )
            }
        }
    }

    fun deleteCurrentSlide() {
        val current = _state.value as? SlidesViewerUiState.Content ?: return
        val slide = current.currentSlide ?: return
        timerJob?.cancel()

        viewModelScope.launch {
            repository.deleteGoalSlide(slide.id).onSuccess {
                val remaining = current.slides.filterNot { it.id == slide.id }
                if (remaining.isEmpty()) {
                    _state.value = SlidesViewerUiState.Closed
                } else {
                    val nextIdx = current.currentIndex.coerceAtMost(remaining.size - 1)
                    _state.value = current.copy(slides = remaining, currentIndex = nextIdx, progress = 0f)
                    startTimer()
                }
            }.onFailure { err ->
                val latest = _state.value as? SlidesViewerUiState.Content ?: return@onFailure
                _state.value = latest.copy(actionError = err.message)
                startTimer()
            }
        }
    }

    fun clearActionError() {
        val current = _state.value as? SlidesViewerUiState.Content ?: return
        _state.value = current.copy(actionError = null)
    }

    class Factory(private val goalId: String) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SlidesViewerViewModel(goalId) as T
        }
    }
}
