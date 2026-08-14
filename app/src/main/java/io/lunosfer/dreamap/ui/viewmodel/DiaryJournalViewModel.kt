package io.lunosfer.dreamap.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.lunosfer.dreamap.DreamapApp
import io.lunosfer.dreamap.R
import io.lunosfer.dreamap.data.model.DiaryEntry
import io.lunosfer.dreamap.data.model.UserProfile
import io.lunosfer.dreamap.data.repository.DiaryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// groupedEntries anahtarı olarak "yyyy-MM-dd" (createdAt'ın ilk 10 karakteri)
// veya tarih yoksa UNKNOWN_DATE_KEY kullanılır — bu ham anahtar hiçbir zaman
// doğrudan ekrana yazılmaz. İnsan-okur etikete ("Bugün"/"Dün"/"10 Ağustos
// 2026") çevirme işi DiaryJournalScreen'de, stringResource ile birlikte
// yapılır; ViewModel'in kendisi yalnızca Context gerektirmeyen hata
// mesajlarında DreamapApp.instance.getString(...) kullanır (ProfileViewModel
// ile aynı desen).
const val UNKNOWN_DATE_KEY = "unknown"

sealed class DiaryJournalUiState {
    object Loading : DiaryJournalUiState()
    data class Error(val message: String) : DiaryJournalUiState()
    data class Success(
        val owner: UserProfile?,
        val groupedEntries: Map<String, List<DiaryEntry>>,
        val isSelf: Boolean
    ) : DiaryJournalUiState()
}

class DiaryJournalViewModel(
    private val userId: String,
    private val repository: DiaryRepository = DiaryRepository()
) : ViewModel() {

    private val _state = MutableStateFlow<DiaryJournalUiState>(DiaryJournalUiState.Loading)
    val state: StateFlow<DiaryJournalUiState> = _state.asStateFlow()

    init {
        loadEntries()
    }

    fun loadEntries() {
        viewModelScope.launch {
            _state.value = DiaryJournalUiState.Loading
            repository.getEntriesForUser(userId)
                .onSuccess { response ->
                    val sorted = response.entries.sortedByDescending { it.createdAt ?: "" }
                    val grouped = sorted.groupBy { entry ->
                        entry.createdAt?.take(10) ?: UNKNOWN_DATE_KEY
                    }
                    _state.value = DiaryJournalUiState.Success(
                        owner = response.owner,
                        groupedEntries = grouped,
                        isSelf = response.isSelf
                    )
                }
                .onFailure { error ->
                    val fallback = DreamapApp.instance.getString(R.string.diary_journal_load_error)
                    _state.value = DiaryJournalUiState.Error(error.message ?: fallback)
                }
        }
    }

    class Factory(private val userId: String) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DiaryJournalViewModel(userId) as T
        }
    }
}
