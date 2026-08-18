package io.lunosfer.dreamap.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.lunosfer.dreamap.data.model.Goal
import io.lunosfer.dreamap.data.repository.VisionRepository
import io.lunosfer.dreamap.supabase.supabaseClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * components/VisionVideoPlayer.jsx'in Android karşılığı — ama BİLİNÇLİ
 * olarak küçük tutuldu: web'deki kaydırarak sıradaki videoya geçme kuyruğu,
 * yorum sheet'i, paylaşım ve bildir menüsü bu ilk sürümde YOK. Sadece: tek
 * vizyonun videosu, oynat/duraklat, çift dokununca beğen, mana ver/kaldır,
 * kaydet, (sahipse) düzenlemeye dön. GoalDetailScreen'deki "Vizyonu İzle"
 * butonundan, goal.visionVideoUrl doluysa buraya gelinir.
 */
sealed class VisionVideoPlayerUiState {
    object Loading : VisionVideoPlayerUiState()
    data class Content(
        val goal: Goal,
        val isOwner: Boolean = false,
        val hasReacted: Boolean = false,
        val believersCount: Int = 0,
        val hasSaved: Boolean = false,
        val actionError: String? = null
    ) : VisionVideoPlayerUiState()
    data class Error(val message: String) : VisionVideoPlayerUiState()
}

class VisionVideoPlayerViewModel(
    private val goalId: String,
    private val repository: VisionRepository = VisionRepository()
) : ViewModel() {

    private val _state = MutableStateFlow<VisionVideoPlayerUiState>(VisionVideoPlayerUiState.Loading)
    val state: StateFlow<VisionVideoPlayerUiState> = _state.asStateFlow()

    private val currentUserId: String?
        get() = supabaseClient.auth.currentUserOrNull()?.id

    init {
        loadGoal()
    }

    fun loadGoal() {
        viewModelScope.launch {
            _state.value = VisionVideoPlayerUiState.Loading
            runCatching {
                supabaseClient.postgrest["goals"]
                    .select(Columns.raw("*, user_profiles:user_id(*)")) {
                        filter { eq("id", goalId) }
                    }.decodeSingle<Goal>()
            }.onSuccess { goal ->
                if (goal.visionVideoUrl.isNullOrBlank()) {
                    _state.value = VisionVideoPlayerUiState.Error(
                        io.lunosfer.dreamap.DreamapApp.instance.getString(io.lunosfer.dreamap.R.string.error_video_not_found)
                    )
                } else {
                    _state.value = VisionVideoPlayerUiState.Content(
                        goal = goal,
                        isOwner = currentUserId != null && goal.userId == currentUserId,
                        hasReacted = goal.hasReacted ?: false,
                        believersCount = goal.believersCount ?: 0,
                        hasSaved = goal.hasSaved ?: false
                    )                }
            }.onFailure { err ->
                _state.value = VisionVideoPlayerUiState.Error(
                    err.message ?: io.lunosfer.dreamap.DreamapApp.instance.getString(io.lunosfer.dreamap.R.string.error_slides_load)
                )
            }
        }
    }

    fun toggleMana() {
        val current = _state.value as? VisionVideoPlayerUiState.Content ?: return
        if (current.isOwner) return
        val wasReacted = current.hasReacted
        val oldCount = current.believersCount

        if (wasReacted) {
            _state.value = current.copy(hasReacted = false, believersCount = maxOf(0, oldCount - 1))
            viewModelScope.launch {
                repository.removeMana(goalId).onFailure {
                    val latest = _state.value as? VisionVideoPlayerUiState.Content ?: return@onFailure
                    _state.value = latest.copy(hasReacted = wasReacted, believersCount = oldCount)
                }
            }
        } else {
            _state.value = current.copy(hasReacted = true, believersCount = oldCount + 1)
            viewModelScope.launch {
                repository.giveMana(goalId, 1).onFailure { err ->
                    val latest = _state.value as? VisionVideoPlayerUiState.Content ?: return@onFailure
                    _state.value = latest.copy(
                        hasReacted = wasReacted,
                        believersCount = oldCount,
                        actionError = err.message
                    )
                }
            }
        }
    }

    /** Çift dokununca beğen — zaten reacted ise tekrar tetiklemeye gerek yok. */
    fun likeOnDoubleTap() {
        val current = _state.value as? VisionVideoPlayerUiState.Content ?: return
        if (!current.hasReacted) toggleMana()
    }

    fun toggleSave() {
        val current = _state.value as? VisionVideoPlayerUiState.Content ?: return
        val wasSaved = current.hasSaved
        _state.value = current.copy(hasSaved = !wasSaved)

        viewModelScope.launch {
            repository.saveGoal(goalId).onSuccess { isSaved ->
                val latest = _state.value as? VisionVideoPlayerUiState.Content ?: return@onSuccess
                _state.value = latest.copy(hasSaved = isSaved)
            }.onFailure { err ->
                val latest = _state.value as? VisionVideoPlayerUiState.Content ?: return@onFailure
                _state.value = latest.copy(hasSaved = wasSaved, actionError = err.message)
            }
        }
    }

    class Factory(private val goalId: String) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return VisionVideoPlayerViewModel(goalId) as T
        }
    }
}
