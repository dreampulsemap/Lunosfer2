package io.lunosfer.dreamap.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.jan.supabase.auth.auth
import io.lunosfer.dreamap.data.model.Dream
import io.lunosfer.dreamap.data.model.PublicProfileData
import io.lunosfer.dreamap.data.repository.FriendsRepository
import io.lunosfer.dreamap.supabase.supabaseClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class PublicProfileUiState {
    object Loading : PublicProfileUiState()
    data class Success(
        val profile: PublicProfileData,
        val dreams: List<Dream> = emptyList(),
        val currentPage: Int = 0,
        val hasMore: Boolean = false,
        val isLoadingMore: Boolean = false,
        val friendshipStatus: String? = null, // null | "pending" | "accepted"
        val followsViewer: Boolean = false,
        val isSelf: Boolean = false,
        val actionMessage: String? = null,
        val actionError: String? = null
    ) : PublicProfileUiState()
    data class Error(val message: String) : PublicProfileUiState()
}

class PublicProfileViewModel(
    private val userId: String,
    private val repository: FriendsRepository = FriendsRepository()
) : ViewModel() {

    private val currentUserId: String?
        get() = supabaseClient.auth.currentUserOrNull()?.id

    private val _state = MutableStateFlow<PublicProfileUiState>(PublicProfileUiState.Loading)
    val state: StateFlow<PublicProfileUiState> = _state.asStateFlow()

    init {
        loadProfile(page = 0)
    }

    fun loadProfile(page: Int = 0) {
        if (page == 0) {
            _state.value = PublicProfileUiState.Loading
        } else {
            val current = _state.value as? PublicProfileUiState.Success ?: return
            _state.value = current.copy(isLoadingMore = true)
        }

        viewModelScope.launch {
            repository.getPublicProfile(userId = userId, page = page).onSuccess { res ->
                val prof = res.profile ?: PublicProfileData(id = userId)
                val current = _state.value as? PublicProfileUiState.Success

                val combinedDreams = if (page == 0 || current == null) {
                    res.dreams
                } else {
                    current.dreams + res.dreams
                }

                _state.value = PublicProfileUiState.Success(
                    profile = prof,
                    dreams = combinedDreams,
                    currentPage = page,
                    hasMore = res.hasMore,
                    isLoadingMore = false,
                    friendshipStatus = res.friendshipStatus,
                    followsViewer = res.followsViewer,
                    isSelf = res.isSelf || (currentUserId != null && currentUserId == userId)
                )
            }.onFailure { err ->
                if (page == 0) {
                    _state.value = PublicProfileUiState.Error(err.message ?: io.lunosfer.dreamap.DreamapApp.instance.getString(io.lunosfer.dreamap.R.string.error_profile_load))
                } else {
                    val current = _state.value as? PublicProfileUiState.Success
                    if (current != null) {
                        _state.value = current.copy(isLoadingMore = false, actionError = err.message ?: "Daha fazla yüklenemedi")
                    }
                }
            }
        }
    }

    fun loadNextPage() {
        val current = _state.value as? PublicProfileUiState.Success ?: return
        if (current.hasMore && !current.isLoadingMore) {
            loadProfile(page = current.currentPage + 1)
        }
    }

    fun sendFollowRequest() {
        val uid = currentUserId ?: return
        val current = _state.value as? PublicProfileUiState.Success ?: return

        viewModelScope.launch {
            repository.sendFriendRequest(userId = uid, friendId = userId).onSuccess { res ->
                val latest = _state.value as? PublicProfileUiState.Success ?: return@onSuccess
                val newStatus = res.status ?: "pending"
                val msg = if (newStatus == "accepted") "Takip edildi" else "Takip isteği gönderildi"
                _state.value = latest.copy(
                    friendshipStatus = newStatus,
                    actionMessage = msg
                )
            }.onFailure { err ->
                val latest = _state.value as? PublicProfileUiState.Success ?: return@onFailure
                _state.value = latest.copy(actionError = err.message ?: "Takip isteği gönderilemedi")
            }
        }
    }

    fun clearActionMessage() {
        val current = _state.value as? PublicProfileUiState.Success ?: return
        _state.value = current.copy(actionMessage = null)
    }

    fun clearActionError() {
        val current = _state.value as? PublicProfileUiState.Success ?: return
        _state.value = current.copy(actionError = null)
    }

    class Factory(private val userId: String) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PublicProfileViewModel(userId) as T
        }
    }
}
