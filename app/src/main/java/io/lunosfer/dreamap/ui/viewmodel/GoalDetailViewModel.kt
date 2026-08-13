package io.lunosfer.dreamap.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.lunosfer.dreamap.data.model.Goal
import io.lunosfer.dreamap.data.model.GoalComment
import io.lunosfer.dreamap.data.repository.VisionRepository
import io.lunosfer.dreamap.supabase.supabaseClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class GoalDetailUiState {
    object Loading : GoalDetailUiState()
    data class Success(
        val goal: Goal,
        val comments: List<GoalComment> = emptyList(),
        val isLoadingComments: Boolean = false,
        val isSubmittingComment: Boolean = false,
        val hasSaved: Boolean = false,
        val hasReacted: Boolean = false,
        val believersCount: Int = 0,
        val actionError: String? = null,
        val actionMessage: String? = null
    ) : GoalDetailUiState()
    data class Error(val message: String) : GoalDetailUiState()
}

class GoalDetailViewModel(
    private val goalId: String,
    private val repository: VisionRepository = VisionRepository()
) : ViewModel() {

    private val _state = MutableStateFlow<GoalDetailUiState>(GoalDetailUiState.Loading)
    val state: StateFlow<GoalDetailUiState> = _state.asStateFlow()

    init {
        loadGoal()
    }

    fun loadGoal() {
        _state.value = GoalDetailUiState.Loading
        viewModelScope.launch {
            // First check feed list
            val feedResult = repository.loadFirstPage()
            val foundInFeed = feedResult.getOrNull()?.find { it.id == goalId }

            if (foundInFeed != null) {
                setGoalSuccess(foundInFeed)
            } else {
                // Fallback: Postgrest fetch
                runCatching {
                    supabaseClient.postgrest["goals"]
                        .select(Columns.raw("*, micro_goals(*), user_profiles:user_id(*)")) {
                            filter {
                                eq("id", goalId)
                            }
                        }.decodeSingle<Goal>()
                }.onSuccess { goal ->
                    setGoalSuccess(goal)
                }.onFailure { err ->
                    _state.value = GoalDetailUiState.Error(io.lunosfer.dreamap.DreamapApp.instance.getString(io.lunosfer.dreamap.R.string.error_vision_not_found, err.message ?: ""))
                }
            }
        }
    }

    private fun setGoalSuccess(goal: Goal) {
        val successState = GoalDetailUiState.Success(
            goal = goal,
            hasSaved = goal.hasSaved ?: false,
            hasReacted = goal.hasReacted ?: false,
            believersCount = goal.believersCount ?: 0,
            isLoadingComments = true
        )
        _state.value = successState
        loadComments()
    }

    fun loadComments() {
        val current = _state.value as? GoalDetailUiState.Success ?: return
        viewModelScope.launch {
            repository.getGoalComments(goalId).onSuccess { commentsList ->
                val latest = _state.value as? GoalDetailUiState.Success ?: return@onSuccess
                _state.value = latest.copy(
                    comments = commentsList,
                    isLoadingComments = false
                )
            }.onFailure {
                val latest = _state.value as? GoalDetailUiState.Success ?: return@onFailure
                _state.value = latest.copy(isLoadingComments = false)
            }
        }
    }

    fun toggleSave() {
        val current = _state.value as? GoalDetailUiState.Success ?: return
        val wasSaved = current.hasSaved
        _state.value = current.copy(hasSaved = !wasSaved)

        viewModelScope.launch {
            repository.saveGoal(goalId).onSuccess { isSaved ->
                val latest = _state.value as? GoalDetailUiState.Success ?: return@onSuccess
                _state.value = latest.copy(
                    hasSaved = isSaved,
                    actionMessage = if (isSaved) "Vizyon kaydedildi" else "Kayıt kaldırıldı"
                )
            }.onFailure { err ->
                val latest = _state.value as? GoalDetailUiState.Success ?: return@onFailure
                _state.value = latest.copy(
                    hasSaved = wasSaved,
                    actionError = err.message ?: "Kayıt işlemi başarısız"
                )
            }
        }
    }

    fun giveMana(amount: Int = 1) {
        val current = _state.value as? GoalDetailUiState.Success ?: return
        val wasReacted = current.hasReacted
        val oldCount = current.believersCount

        // Optimistic update
        _state.value = current.copy(
            hasReacted = true,
            believersCount = oldCount + amount
        )

        viewModelScope.launch {
            repository.giveMana(goalId, amount).onSuccess { res ->
                val latest = _state.value as? GoalDetailUiState.Success ?: return@onSuccess
                val msg = "Vizyona Mana aktarıldı! (Kalan Mana: ${res.manaBalance ?: "—"})"
                _state.value = latest.copy(actionMessage = msg)
            }.onFailure { err ->
                val latest = _state.value as? GoalDetailUiState.Success ?: return@onFailure
                val msg = err.message ?: ""
                val errText = when {
                    msg.contains("insufficient_mana", ignoreCase = true) || msg.contains("402") -> "Yetersiz Mana"
                    msg.contains("cannot_react_to_own_goal", ignoreCase = true) -> "Kendi hedefinize mana veremezsiniz"
                    msg.contains("already_reacted", ignoreCase = true) -> "Bu hedefe zaten mana verdiniz"
                    else -> err.message ?: "Mana verilemedi"
                }
                // Rollback
                _state.value = latest.copy(
                    hasReacted = wasReacted,
                    believersCount = oldCount,
                    actionError = errText
                )
            }
        }
    }

    fun removeMana() {
        val current = _state.value as? GoalDetailUiState.Success ?: return
        val wasReacted = current.hasReacted
        val oldCount = current.believersCount

        _state.value = current.copy(
            hasReacted = false,
            believersCount = maxOf(0, oldCount - 1)
        )

        viewModelScope.launch {
            repository.removeMana(goalId).onSuccess {
                val latest = _state.value as? GoalDetailUiState.Success ?: return@onSuccess
                _state.value = latest.copy(actionMessage = "Mana reaksiyonu kaldırıldı")
            }.onFailure { err ->
                val latest = _state.value as? GoalDetailUiState.Success ?: return@onFailure
                _state.value = latest.copy(
                    hasReacted = wasReacted,
                    believersCount = oldCount,
                    actionError = err.message ?: "İşlem başarısız"
                )
            }
        }
    }

    fun addComment(content: String) {
        if (content.isBlank()) return
        val current = _state.value as? GoalDetailUiState.Success ?: return
        _state.value = current.copy(isSubmittingComment = true)

        viewModelScope.launch {
            repository.createGoalComment(goalId, content.trim()).onSuccess { newComment ->
                val latest = _state.value as? GoalDetailUiState.Success ?: return@onSuccess
                _state.value = latest.copy(
                    comments = latest.comments + newComment,
                    isSubmittingComment = false
                )
            }.onFailure { err ->
                val latest = _state.value as? GoalDetailUiState.Success ?: return@onFailure
                _state.value = latest.copy(
                    isSubmittingComment = false,
                    actionError = err.message ?: "Yorum eklenemedi"
                )
            }
        }
    }

    fun deleteComment(commentId: String) {
        viewModelScope.launch {
            repository.deleteGoalComment(commentId).onSuccess {
                val latest = _state.value as? GoalDetailUiState.Success ?: return@onSuccess
                _state.value = latest.copy(
                    comments = latest.comments.filterNot { it.id == commentId }
                )
            }.onFailure { err ->
                setActionError(err.message ?: "Yorum silinemedi")
            }
        }
    }

    fun updateStatus(status: String, story: String?, onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.updateGoalStatus(goalId, status, story).onSuccess { updatedGoal ->
                val current = _state.value as? GoalDetailUiState.Success
                if (current != null) {
                    _state.value = current.copy(
                        goal = updatedGoal,
                        actionMessage = "Vizyon durumu güncellendi"
                    )
                }
                onComplete()
            }.onFailure { err ->
                val msg = err.message ?: ""
                val errText = when {
                    msg.contains("not_owner", ignoreCase = true) || msg.contains("403") -> "Bu işlem için yetkiniz yok"
                    msg.contains("goal_already_resolved", ignoreCase = true) -> "Tamamlanmış veya bırakılmış bir hedef değiştirilemez"
                    else -> err.message ?: "Durum güncellenemedi"
                }
                setActionError(errText)
            }
        }
    }

    fun deleteGoal(onSuccess: () -> Unit) {
        viewModelScope.launch {
            repository.deleteGoal(goalId).onSuccess {
                onSuccess()
            }.onFailure { err ->
                setActionError(err.message ?: "Silme işlemi başarısız")
            }
        }
    }

    // --- Cover & Gallery Management ---

    fun generateCover() {
        val current = _state.value as? GoalDetailUiState.Success ?: return
        viewModelScope.launch {
            repository.generateGoalCover(goalId, current.goal.title, current.goal.description)
                .onSuccess { newUrl ->
                    _state.value = current.copy(
                        goal = current.goal.copy(coverImageUrl = newUrl),
                        actionMessage = "Yapay zeka kapak görseli oluşturuldu"
                    )
                }
                .onFailure { err ->
                    setActionError(err.message ?: "Kapak üretilemedi")
                }
        }
    }

    fun addPixabayImage(pixabayId: Long, imageUrl: String, tags: String, pixabayUser: String) {
        val current = _state.value as? GoalDetailUiState.Success ?: return
        viewModelScope.launch {
            repository.addGoalImageFromPixabay(goalId, pixabayId, imageUrl, tags, pixabayUser)
                .onSuccess { newUrl ->
                    _state.value = current.copy(
                        goal = current.goal.copy(coverImageUrl = newUrl),
                        actionMessage = "Görsel eklendi"
                    )
                }
                .onFailure { err ->
                    setActionError(err.message ?: "Görsel eklenemedi")
                }
        }
    }

    fun addUrlImage(imageUrl: String) {
        val current = _state.value as? GoalDetailUiState.Success ?: return
        viewModelScope.launch {
            repository.addGoalImage(goalId, imageUrl)
                .onSuccess { newUrl ->
                    _state.value = current.copy(
                        goal = current.goal.copy(coverImageUrl = newUrl),
                        actionMessage = "Görsel eklendi"
                    )
                }
                .onFailure { err ->
                    setActionError(err.message ?: "Görsel eklenemedi")
                }
        }
    }

    fun setCover(imageUrl: String) {
        val current = _state.value as? GoalDetailUiState.Success ?: return
        viewModelScope.launch {
            repository.setGoalCover(goalId, imageUrl)
                .onSuccess { newCoverUrl ->
                    _state.value = current.copy(
                        goal = current.goal.copy(coverImageUrl = newCoverUrl),
                        actionMessage = "Kapak görseli güncellendi"
                    )
                }
                .onFailure { err ->
                    setActionError(err.message ?: "Kapak ayarlanamadı")
                }
        }
    }

    fun removeImage(imageUrl: String) {
        val current = _state.value as? GoalDetailUiState.Success ?: return
        viewModelScope.launch {
            repository.removeGoalImage(goalId, imageUrl)
                .onSuccess {
                    val newCover = if (current.goal.coverImageUrl == imageUrl) null else current.goal.coverImageUrl
                    _state.value = current.copy(
                        goal = current.goal.copy(coverImageUrl = newCover),
                        actionMessage = "Görsel kaldırıldı"
                    )
                }
                .onFailure { err ->
                    setActionError(err.message ?: "Görsel kaldırılamadı")
                }
        }
    }

    // --- Translation ---

    fun translate(text: String, onTranslated: (String) -> Unit) {
        viewModelScope.launch {
            runCatching {
                io.lunosfer.dreamap.data.network.NetworkModule.api.translateText(
                    io.lunosfer.dreamap.data.model.TranslateRequest(text = text, targetLang = "tr")
                )
            }.onSuccess { res ->
                val tr = res.resultText
                if (tr.isNotBlank()) {
                    onTranslated(tr)
                } else {
                    setActionError("Çeviri alınamadı")
                }
            }.onFailure { err ->
                setActionError(err.message ?: "Çeviri başarısız")
            }
        }
    }

    fun setActionError(error: String) {

        val current = _state.value as? GoalDetailUiState.Success ?: return
        _state.value = current.copy(actionError = error)
    }

    fun clearActionError() {
        val current = _state.value as? GoalDetailUiState.Success ?: return
        _state.value = current.copy(actionError = null)
    }

    fun clearActionMessage() {
        val current = _state.value as? GoalDetailUiState.Success ?: return
        _state.value = current.copy(actionMessage = null)
    }

    class Factory(private val goalId: String) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return GoalDetailViewModel(goalId) as T
        }
    }
}
