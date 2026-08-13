package io.lunosfer.dreamap.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.jan.supabase.auth.auth
import io.lunosfer.dreamap.data.model.Friendship
import io.lunosfer.dreamap.data.model.UserSearchResult
import io.lunosfer.dreamap.data.repository.FriendsRepository
import io.lunosfer.dreamap.supabase.supabaseClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AddFriendUiState {
    object Idle : AddFriendUiState()
    data class Content(
        val query: String = "",
        val searchResults: List<UserSearchResult> = emptyList(),
        val isSearching: Boolean = false,
        val pendingRequests: List<Friendship> = emptyList(),
        val isLoadingPending: Boolean = false,
        val actionMessage: String? = null,
        val actionError: String? = null
    ) : AddFriendUiState()
}

class AddFriendViewModel(
    private val repository: FriendsRepository = FriendsRepository()
) : ViewModel() {

    private val currentUserId: String?
        get() = supabaseClient.auth.currentUserOrNull()?.id

    private val _state = MutableStateFlow<AddFriendUiState>(
        AddFriendUiState.Content()
    )
    val state: StateFlow<AddFriendUiState> = _state.asStateFlow()

    init {
        loadPendingRequests()
    }

    fun loadPendingRequests() {
        val uid = currentUserId ?: return
        val current = _state.value as? AddFriendUiState.Content ?: AddFriendUiState.Content()
        _state.value = current.copy(isLoadingPending = true)

        viewModelScope.launch {
            repository.getFriendsList(userId = uid, type = "pending").onSuccess { list ->
                val latest = _state.value as? AddFriendUiState.Content ?: return@onSuccess
                _state.value = latest.copy(
                    pendingRequests = list,
                    isLoadingPending = false
                )
            }.onFailure {
                val latest = _state.value as? AddFriendUiState.Content ?: return@onFailure
                _state.value = latest.copy(isLoadingPending = false)
            }
        }
    }

    fun searchUsers(queryText: String) {
        val uid = currentUserId ?: return
        val current = _state.value as? AddFriendUiState.Content ?: AddFriendUiState.Content()
        _state.value = current.copy(query = queryText, isSearching = true)

        if (queryText.isBlank()) {
            _state.value = current.copy(query = "", searchResults = emptyList(), isSearching = false)
            return
        }

        viewModelScope.launch {
            repository.searchFriends(query = queryText.trim(), userId = uid).onSuccess { users ->
                val latest = _state.value as? AddFriendUiState.Content ?: return@onSuccess
                _state.value = latest.copy(
                    searchResults = users,
                    isSearching = false
                )
            }.onFailure { err ->
                val latest = _state.value as? AddFriendUiState.Content ?: return@onFailure
                _state.value = latest.copy(
                    isSearching = false,
                    actionError = err.message ?: "Arama yapılamadı"
                )
            }
        }
    }

    fun sendFollowRequest(targetUserId: String) {
        val uid = currentUserId ?: return
        val current = _state.value as? AddFriendUiState.Content ?: return

        viewModelScope.launch {
            repository.sendFriendRequest(userId = uid, friendId = targetUserId).onSuccess { res ->
                val latest = _state.value as? AddFriendUiState.Content ?: return@onSuccess
                val newStatus = res.status ?: "pending"
                val updatedResults = latest.searchResults.map { user ->
                    if (user.id == targetUserId) {
                        user.copy(friendshipStatus = newStatus)
                    } else user
                }
                val msg = if (newStatus == "accepted") "Takip edildi" else "Takip isteği gönderildi"
                _state.value = latest.copy(
                    searchResults = updatedResults,
                    actionMessage = msg
                )
            }.onFailure { err ->
                val latest = _state.value as? AddFriendUiState.Content ?: return@onFailure
                _state.value = latest.copy(actionError = err.message ?: "İstek gönderilemedi")
            }
        }
    }

    fun respondToRequest(friendshipId: String, action: String) {
        val uid = currentUserId ?: return
        val current = _state.value as? AddFriendUiState.Content ?: return

        viewModelScope.launch {
            repository.respondToFriendRequest(friendshipId = friendshipId, userId = uid, action = action).onSuccess {
                val latest = _state.value as? AddFriendUiState.Content ?: return@onSuccess
                val updatedPending = latest.pendingRequests.filterNot { it.id == friendshipId }
                val msg = if (action == "accepted") "Takip isteği kabul edildi" else "İstek reddedildi"
                _state.value = latest.copy(
                    pendingRequests = updatedPending,
                    actionMessage = msg
                )
            }.onFailure { err ->
                val latest = _state.value as? AddFriendUiState.Content ?: return@onFailure
                _state.value = latest.copy(actionError = err.message ?: "İşlem gerçekleştirilemedi")
            }
        }
    }

    fun clearActionMessage() {
        val current = _state.value as? AddFriendUiState.Content ?: return
        _state.value = current.copy(actionMessage = null)
    }

    fun clearActionError() {
        val current = _state.value as? AddFriendUiState.Content ?: return
        _state.value = current.copy(actionError = null)
    }
}
