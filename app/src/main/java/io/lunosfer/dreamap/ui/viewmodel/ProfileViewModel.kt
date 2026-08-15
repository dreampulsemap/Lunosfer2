package io.lunosfer.dreamap.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.jan.supabase.auth.auth
import io.lunosfer.dreamap.data.model.Dream
import io.lunosfer.dreamap.data.model.Goal
import io.lunosfer.dreamap.data.model.FullUserProfile
import io.lunosfer.dreamap.data.model.PremiumStatusResponse
import io.lunosfer.dreamap.data.model.UpdateProfileRequest
import io.lunosfer.dreamap.data.repository.ProfileRepository
import io.lunosfer.dreamap.supabase.supabaseClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.async

sealed class ProfileUiState {
    object Loading : ProfileUiState()
    data class Content(
        val profile: FullUserProfile,
        val premiumStatus: PremiumStatusResponse = PremiumStatusResponse(),
        val stats: io.lunosfer.dreamap.data.model.ProfileStatsResponse = io.lunosfer.dreamap.data.model.ProfileStatsResponse(),
        val dreams: List<Dream> = emptyList(),
        val visions: List<Goal> = emptyList(),
        val savedVisions: List<Goal> = emptyList(),
        val selectedTab: Int = 0, // 0: Visions, 1: Dreams, 2: Journal (navigates away, never "selected"), 3: Saved
        val isLoadingPremium: Boolean = false,
        val isSavingProfile: Boolean = false,
        val isEditModalOpen: Boolean = false,
        val actionMessage: String? = null,
        val actionError: String? = null
    ) : ProfileUiState()
    data class Error(val message: String) : ProfileUiState()
}

class ProfileViewModel(
    private val repository: ProfileRepository = ProfileRepository()
) : ViewModel() {

    private val currentUserId: String?
        get() = supabaseClient.auth.currentUserOrNull()?.id

    private val _state = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val state: StateFlow<ProfileUiState> = _state.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        val uid = currentUserId
        if (uid == null) {
            _state.value = ProfileUiState.Error(io.lunosfer.dreamap.DreamapApp.instance.getString(io.lunosfer.dreamap.R.string.error_no_session))
            return
        }

        _state.value = ProfileUiState.Loading

        viewModelScope.launch {
            val profileDef = async { repository.getUserProfile(uid).getOrNull() ?: FullUserProfile(id = uid) }
            val premiumDef = async { repository.getPremiumStatus().getOrNull() ?: PremiumStatusResponse() }
            val statsDef = async { repository.getProfileStats().getOrNull() ?: io.lunosfer.dreamap.data.model.ProfileStatsResponse() }
            val dreamsDef = async { repository.getUserDreams(uid).getOrNull() ?: emptyList() }
            val visionsDef = async { repository.getUserVisions().getOrNull() ?: emptyList() }
            val savedDef = async { repository.getSavedVisions().getOrNull() ?: emptyList() }

            _state.value = ProfileUiState.Content(
                profile = profileDef.await(),
                premiumStatus = premiumDef.await(),
                stats = statsDef.await(),
                dreams = dreamsDef.await(),
                visions = visionsDef.await(),
                savedVisions = savedDef.await()
            )
        }
    }

    fun selectTab(index: Int) {
        val current = _state.value as? ProfileUiState.Content ?: return
        _state.value = current.copy(selectedTab = index)
    }

    fun openEditModal() {
        val current = _state.value as? ProfileUiState.Content ?: return
        _state.value = current.copy(isEditModalOpen = true)
    }

    fun closeEditModal() {
        val current = _state.value as? ProfileUiState.Content ?: return
        _state.value = current.copy(isEditModalOpen = false)
    }

    fun updateProfile(
        username: String,
        displayName: String,
        avatarUrl: String,
        isPrivate: Boolean,
        language: String,
        gender: String
    ) {
        val uid = currentUserId ?: return
        val current = _state.value as? ProfileUiState.Content ?: return

        if (username.isNotBlank() && (username.length < 3 || username.length > 32)) {
            _state.value = current.copy(actionError = "Kullanıcı adı 3 ile 32 karakter arasında olmalıdır.")
            return
        }
        if (displayName.length > 60) {
            _state.value = current.copy(actionError = "Görünen ad en fazla 60 karakter olabilir.")
            return
        }

        _state.value = current.copy(isSavingProfile = true)

        val req = UpdateProfileRequest(
            userId = uid,
            username = username.trim().takeIf { it.isNotBlank() },
            displayName = displayName.trim().takeIf { it.isNotBlank() },
            avatarUrl = avatarUrl.trim().takeIf { it.isNotBlank() },
            isPrivate = isPrivate,
            language = language.takeIf { it.isNotBlank() },
            gender = gender.takeIf { it.isNotBlank() }
        )

        viewModelScope.launch {
            repository.updateProfile(req).onSuccess { updatedProfile ->
                val latest = _state.value as? ProfileUiState.Content ?: return@onSuccess
                _state.value = latest.copy(
                    profile = updatedProfile,
                    isSavingProfile = false,
                    isEditModalOpen = false,
                    actionMessage = "Profil başarıyla güncellendi."
                )
            }.onFailure { err ->
                val latest = _state.value as? ProfileUiState.Content ?: return@onFailure
                _state.value = latest.copy(
                    isSavingProfile = false,
                    actionError = err.message ?: "Profil güncellenemedi."
                )
            }
        }
    }

    fun clearActionMessage() {
        val current = _state.value as? ProfileUiState.Content ?: return
        _state.value = current.copy(actionMessage = null)
    }

    fun clearActionError() {
        val current = _state.value as? ProfileUiState.Content ?: return
        _state.value = current.copy(actionError = null)
    }
}
