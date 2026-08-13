package io.lunosfer.dreamap.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.lunosfer.dreamap.data.model.AppNotification
import io.lunosfer.dreamap.data.repository.NotificationsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class NotificationsUiState {
    object Loading : NotificationsUiState()
    data class Success(
        val notifications: List<AppNotification> = emptyList(),
        val unreadCount: Int = 0,
        val isMarkingRead: Boolean = false,
        val actionError: String? = null
    ) : NotificationsUiState()
    data class Error(val message: String) : NotificationsUiState()
}

class NotificationsViewModel(
    private val repository: NotificationsRepository = NotificationsRepository()
) : ViewModel() {

    private val _state = MutableStateFlow<NotificationsUiState>(NotificationsUiState.Loading)
    val state: StateFlow<NotificationsUiState> = _state.asStateFlow()

    init {
        loadNotifications()
    }

    fun loadNotifications() {
        _state.value = NotificationsUiState.Loading
        viewModelScope.launch {
            repository.getNotifications().onSuccess { res ->
                _state.value = NotificationsUiState.Success(
                    notifications = res.notifications,
                    unreadCount = res.unreadCount
                )
            }.onFailure { err ->
                _state.value = NotificationsUiState.Error(err.message ?: io.lunosfer.dreamap.DreamapApp.instance.getString(io.lunosfer.dreamap.R.string.error_notifications_load))
            }
        }
    }

    fun markAsRead(notificationId: String? = null, onComplete: () -> Unit = {}) {
        val current = _state.value as? NotificationsUiState.Success ?: return
        _state.value = current.copy(isMarkingRead = true)

        viewModelScope.launch {
            repository.markNotificationsRead(notificationId).onSuccess {
                val latest = _state.value as? NotificationsUiState.Success ?: return@onSuccess
                val updatedNotifications = if (notificationId == null) {
                    latest.notifications.map { it.copy(isRead = true) }
                } else {
                    latest.notifications.map {
                        if (it.id == notificationId) it.copy(isRead = true) else it
                    }
                }
                val newUnread = if (notificationId == null) 0 else maxOf(0, latest.unreadCount - 1)

                _state.value = latest.copy(
                    notifications = updatedNotifications,
                    unreadCount = newUnread,
                    isMarkingRead = false
                )
                onComplete()
            }.onFailure { err ->
                val latest = _state.value as? NotificationsUiState.Success ?: return@onFailure
                _state.value = latest.copy(
                    isMarkingRead = false,
                    actionError = err.message ?: "Okundu işaretlenemedi"
                )
                onComplete()
            }
        }
    }

    fun clearActionError() {
        val current = _state.value as? NotificationsUiState.Success ?: return
        _state.value = current.copy(actionError = null)
    }
}
