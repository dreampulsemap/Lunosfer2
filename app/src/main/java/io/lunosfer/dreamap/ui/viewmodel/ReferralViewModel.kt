package io.lunosfer.dreamap.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.lunosfer.dreamap.data.model.ReferralStatsResponse
import io.lunosfer.dreamap.data.repository.ReferralRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class ReferralUiState {
    object Idle : ReferralUiState()
    object Loading : ReferralUiState()
    data class Success(val stats: ReferralStatsResponse) : ReferralUiState()
    data class Error(val message: String) : ReferralUiState()
}

class ReferralViewModel(
    private val repository: ReferralRepository = ReferralRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<ReferralUiState>(ReferralUiState.Idle)
    val uiState: StateFlow<ReferralUiState> = _uiState.asStateFlow()

    private val _claimMessage = MutableStateFlow<String?>(null)
    val claimMessage: StateFlow<String?> = _claimMessage.asStateFlow()

    private val _claimError = MutableStateFlow<String?>(null)
    val claimError: StateFlow<String?> = _claimError.asStateFlow()

    private val _isClaiming = MutableStateFlow(false)
    val isClaiming: StateFlow<Boolean> = _isClaiming.asStateFlow()

    init {
        loadStats()
    }

    fun loadStats() {
        _uiState.value = ReferralUiState.Loading
        viewModelScope.launch {
            repository.getReferralStats()
                .onSuccess { stats ->
                    _uiState.value = ReferralUiState.Success(stats)
                }
                .onFailure { err ->
                    _uiState.value = ReferralUiState.Error(err.message ?: io.lunosfer.dreamap.DreamapApp.instance.getString(io.lunosfer.dreamap.R.string.error_referral_stats))
                }
        }
    }

    fun claimCode(code: String) {
        if (code.isBlank()) return
        _isClaiming.value = true
        _claimMessage.value = null
        _claimError.value = null

        viewModelScope.launch {
            repository.claimReferral(code.trim())
                .onSuccess { res ->
                    _isClaiming.value = false
                    val awarded = res.manaAwarded
                    val msg = res.message ?: if (awarded > 0) io.lunosfer.dreamap.DreamapApp.instance.getString(io.lunosfer.dreamap.R.string.msg_mana_awarded, awarded) else io.lunosfer.dreamap.DreamapApp.instance.getString(io.lunosfer.dreamap.R.string.msg_referral_applied)
                    _claimMessage.value = msg
                    loadStats()
                }
                .onFailure { err ->
                    _isClaiming.value = false
                    _claimError.value = err.message ?: "Davet kodu kullanılamadı"
                }
        }
    }

    fun clearMessages() {
        _claimMessage.value = null
        _claimError.value = null
    }
}
