package io.lunosfer.dreamap.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.lunosfer.dreamap.data.model.*
import io.lunosfer.dreamap.data.repository.DreamRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class DreamDetailUiState {
    object Loading : DreamDetailUiState()
    data class Success(
        val dream: DreamDetail,
        val comments: List<DreamComment> = emptyList(),
        val isLoadingComments: Boolean = false,
        val isSubmittingComment: Boolean = false,
        val isLiked: Boolean = false,
        val likesCount: Int = 0,
        val commentsCount: Int = 0,
        val bounty: Int = 0,
        val actionError: String? = null,
        val actionMessage: String? = null,
        val isGeneratingDeepAnalysis: Boolean = false,
        val deepAnalysisResult: String? = null
    ) : DreamDetailUiState()
    data class Error(val message: String) : DreamDetailUiState()
}

class DreamDetailViewModel : ViewModel() {
    private val repository = DreamRepository()
    private val _state = MutableStateFlow<DreamDetailUiState>(DreamDetailUiState.Loading)
    val state: StateFlow<DreamDetailUiState> = _state.asStateFlow()

    fun loadDream(id: Long) {
        _state.value = DreamDetailUiState.Loading
        viewModelScope.launch {
            repository.getDream(id).onSuccess { dream ->
                val successState = DreamDetailUiState.Success(
                    dream = dream,
                    isLiked = dream.effectiveIsLiked,
                    likesCount = dream.likesCount,
                    commentsCount = dream.commentsCount,
                    bounty = dream.effectiveBounty,
                    isLoadingComments = true
                )
                _state.value = successState
                loadComments(id)
            }.onFailure { error ->
                _state.value = DreamDetailUiState.Error(error.message ?: io.lunosfer.dreamap.DreamapApp.instance.getString(io.lunosfer.dreamap.R.string.error_dream_load))
            }
        }
    }

    fun loadComments(dreamId: Long) {
        val currentState = _state.value as? DreamDetailUiState.Success ?: return
        viewModelScope.launch {
            repository.getComments(dreamId).onSuccess { commentsList ->
                val latest = _state.value as? DreamDetailUiState.Success ?: return@onSuccess
                _state.value = latest.copy(
                    comments = commentsList,
                    isLoadingComments = false
                )
            }.onFailure {
                val latest = _state.value as? DreamDetailUiState.Success ?: return@onFailure
                _state.value = latest.copy(isLoadingComments = false)
            }
        }
    }

    fun toggleLike(dreamId: Long, userId: String?) {
        if (userId.isNullOrBlank()) {
            setActionError("Beğenmek için giriş yapmalısınız")
            return
        }
        val current = _state.value as? DreamDetailUiState.Success ?: return
        val wasLiked = current.isLiked
        val oldCount = current.likesCount

        // Optimistic update
        val newLiked = !wasLiked
        val newCount = if (newLiked) oldCount + 1 else maxOf(0, oldCount - 1)
        _state.value = current.copy(isLiked = newLiked, likesCount = newCount)

        viewModelScope.launch {
            val result = if (newLiked) {
                repository.likeDream(dreamId, userId)
            } else {
                repository.unlikeDream(dreamId, userId)
            }

            result.onSuccess { response ->
                val latest = _state.value as? DreamDetailUiState.Success ?: return@onSuccess
                val confirmedCount = response.count ?: newCount
                _state.value = latest.copy(
                    isLiked = response.liked,
                    likesCount = confirmedCount
                )
            }.onFailure { err ->
                val msg = err.message ?: ""
                if (msg.contains("Already liked", ignoreCase = true)) {
                    // Sessizce yut, optimistic state zaten doğru
                    return@onFailure
                }
                // Hata durumunda rollback
                val latest = _state.value as? DreamDetailUiState.Success ?: return@onFailure
                _state.value = latest.copy(
                    isLiked = wasLiked,
                    likesCount = oldCount,
                    actionError = "Beğeni işlemi başarısız"
                )
            }
        }
    }

    fun addComment(dreamId: Long, userId: String?, content: String) {
        if (userId.isNullOrBlank()) {
            setActionError("Yorum yapmak için giriş yapmalısınız")
            return
        }
        if (content.isBlank()) return

        val current = _state.value as? DreamDetailUiState.Success ?: return
        _state.value = current.copy(isSubmittingComment = true)

        viewModelScope.launch {
            repository.createComment(dreamId, userId, content.trim()).onSuccess { newComment ->
                val latest = _state.value as? DreamDetailUiState.Success ?: return@onSuccess
                val updatedComments = if (newComment != null) latest.comments + newComment else latest.comments
                _state.value = latest.copy(
                    comments = updatedComments,
                    commentsCount = latest.commentsCount + 1,
                    isSubmittingComment = false
                )
            }.onFailure { err ->
                val latest = _state.value as? DreamDetailUiState.Success ?: return@onFailure
                _state.value = latest.copy(
                    isSubmittingComment = false,
                    actionError = err.message ?: "Yorum eklenemedi"
                )
            }
        }
    }

    fun deleteComment(commentId: Long, userId: String) {
        val current = _state.value as? DreamDetailUiState.Success ?: return
        viewModelScope.launch {
            repository.deleteComment(commentId, userId).onSuccess {
                val latest = _state.value as? DreamDetailUiState.Success ?: return@onSuccess
                _state.value = latest.copy(
                    comments = latest.comments.filterNot { it.id == commentId },
                    commentsCount = maxOf(0, latest.commentsCount - 1)
                )
            }.onFailure { err ->
                setActionError(err.message ?: "Yorum silinemedi")
            }
        }
    }

    fun updateDream(request: UpdateDreamRequest, onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.updateDream(request).onSuccess {
                val current = _state.value as? DreamDetailUiState.Success
                if (current != null) {
                    val updatedDream = current.dream.copy(
                        content = request.content ?: current.dream.content,
                        locationName = request.locationName ?: current.dream.locationName,
                        visibility = request.visibility ?: current.dream.visibility,
                        inFeed = request.inFeed ?: current.dream.inFeed,
                        tags = request.tags ?: current.dream.tags
                    )
                    _state.value = current.copy(
                        dream = updatedDream,
                        actionMessage = io.lunosfer.dreamap.DreamapApp.instance.getString(io.lunosfer.dreamap.R.string.msg_dream_updated)
                    )
                }
                onComplete()
            }.onFailure { err ->
                setActionError(err.message ?: "Güncelleme başarısız")
            }
        }
    }

    fun deleteDream(dreamId: Long, userId: String, softDelete: Boolean, onSuccess: () -> Unit) {
        viewModelScope.launch {
            repository.deleteDream(dreamId, userId, softDelete).onSuccess {
                onSuccess()
            }.onFailure { err ->
                setActionError(err.message ?: "Silme işlemi başarısız")
            }
        }
    }

    fun boostDream(dreamId: Long) {
        viewModelScope.launch {
            repository.boostDream(dreamId).onSuccess { res ->
                val current = _state.value as? DreamDetailUiState.Success ?: return@onSuccess
                val msg = io.lunosfer.dreamap.DreamapApp.instance.getString(io.lunosfer.dreamap.R.string.msg_dream_boosted, res.aurasLeft?.toString() ?: "—")
                _state.value = current.copy(actionMessage = msg)
            }.onFailure { err ->
                val msg = err.message ?: ""
                if (msg.contains("no_auras", ignoreCase = true) || msg.contains("402")) {
                    setActionError("Yetersiz Aura")
                } else if (msg.contains("forbidden", ignoreCase = true) || msg.contains("403")) {
                    setActionError("Bu işlem için yetkiniz yok")
                } else {
                    setActionError("Parlatma işlemi başarısız")
                }
            }
        }
    }

    fun addBounty(dreamId: Long, amount: Int) {
        if (amount < 1) {
            setActionError("Geçerli bir ödül miktarı girin")
            return
        }
        viewModelScope.launch {
            repository.addBounty(dreamId, amount).onSuccess { res ->
                val current = _state.value as? DreamDetailUiState.Success ?: return@onSuccess
                val newTotal = res.newBounty ?: (current.bounty + amount)
                val msg = io.lunosfer.dreamap.DreamapApp.instance.getString(io.lunosfer.dreamap.R.string.msg_dream_bounty_added, amount, res.aurasLeft?.toString() ?: "—")
                _state.value = current.copy(
                    bounty = newTotal,
                    actionMessage = msg
                )
            }.onFailure { err ->
                val msg = err.message ?: ""
                if (msg.contains("no_auras", ignoreCase = true) || msg.contains("402")) {
                    setActionError("Yetersiz Aura")
                } else if (msg.contains("forbidden", ignoreCase = true) || msg.contains("403")) {
                    setActionError("Bu işlem için yetkiniz yok")
                } else {
                    setActionError("Ödül eklenemedi")
                }
            }
        }
    }

    fun analyzeDream(id: Long, content: String, lang: String) {
        viewModelScope.launch {
            repository.analyzeDream(id, content, lang)
            loadDream(id)
        }
    }

    fun requestDeepAnalysis(dreamId: Long) {
        val current = _state.value as? DreamDetailUiState.Success ?: return
        _state.value = current.copy(isGeneratingDeepAnalysis = true)
        viewModelScope.launch {
            repository.generateDeepAnalysis(dreamId).onSuccess { response ->
                val latest = _state.value as? DreamDetailUiState.Success ?: return@onSuccess
                val text = response.resultText ?: "Derin analiz başarıyla tamamlandı."
                _state.value = latest.copy(
                    isGeneratingDeepAnalysis = false,
                    deepAnalysisResult = text,
                    actionMessage = "Derin analiz hazırlandı!"
                )
                repository.getDream(dreamId).onSuccess { updatedDream ->
                    val latest2 = _state.value as? DreamDetailUiState.Success ?: return@onSuccess
                    _state.value = latest2.copy(dream = updatedDream)
                }
            }.onFailure { error ->
                val latest = _state.value as? DreamDetailUiState.Success ?: return@onFailure
                val msg = error.message ?: ""
                val errText = when {
                    msg.contains("no_auras", ignoreCase = true) || msg.contains("402") -> "Derin analiz için Aura yetersiz."
                    else -> "Derin analiz üretilemedi."
                }
                _state.value = latest.copy(
                    isGeneratingDeepAnalysis = false,
                    actionError = errText
                )
            }
        }
    }

    fun setActionError(error: String) {
        val current = _state.value as? DreamDetailUiState.Success ?: return
        _state.value = current.copy(actionError = error)
    }

    fun clearActionError() {
        val current = _state.value as? DreamDetailUiState.Success ?: return
        _state.value = current.copy(actionError = null)
    }

    fun clearActionMessage() {
        val current = _state.value as? DreamDetailUiState.Success ?: return
        _state.value = current.copy(actionMessage = null)
    }
}

