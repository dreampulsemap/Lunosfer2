package io.lunosfer.dreamap.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.lunosfer.dreamap.data.model.GoalSlide
import io.lunosfer.dreamap.data.model.UpdateSlideRequest
import io.lunosfer.dreamap.data.repository.VisionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

const val MAX_GOAL_SLIDES = 20

sealed class SlideCreatorUiState {
    object Loading : SlideCreatorUiState()
    data class Content(
        val slides: List<GoalSlide> = emptyList(),
        val isUploading: Boolean = false,
        val editingSlideId: String? = null, // null = düzenlenen yok
        val error: String? = null,
        val infoMessage: String? = null
    ) : SlideCreatorUiState() {
        val editingSlide: GoalSlide? get() = slides.find { it.id == editingSlideId }
        val canAddMore: Boolean get() = slides.size < MAX_GOAL_SLIDES
    }
    data class Error(val message: String) : SlideCreatorUiState()
}

class SlideCreatorViewModel(
    private val goalId: String,
    private val repository: VisionRepository = VisionRepository()
) : ViewModel() {

    private val _state = MutableStateFlow<SlideCreatorUiState>(SlideCreatorUiState.Loading)
    val state: StateFlow<SlideCreatorUiState> = _state.asStateFlow()

    init {
        loadSlides()
    }

    fun loadSlides() {
        viewModelScope.launch {
            _state.value = SlideCreatorUiState.Loading
            repository.loadGoalSlides(goalId).onSuccess { res ->
                _state.value = SlideCreatorUiState.Content(
                    slides = res.slides.sortedBy { it.orderIndex ?: 0 }
                )
            }.onFailure { err ->
                // Boş liste de "başarı" sayılır (henüz slayt eklenmemiş goal) —
                // sadece gerçek bir hata durumunda Error state'ine düş.
                _state.value = SlideCreatorUiState.Content(slides = emptyList())
            }
        }
    }

    /** Cihazdan seçilen görseli yükler ve boş bir slayt olarak ekler. */
    fun addSlideFromDevice(context: Context, uri: Uri) {
        val current = _state.value as? SlideCreatorUiState.Content ?: return
        if (!current.canAddMore) {
            _state.value = current.copy(error = "En fazla $MAX_GOAL_SLIDES slayt ekleyebilirsin.")
            return
        }
        _state.value = current.copy(isUploading = true, error = null)

        viewModelScope.launch {
            try {
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                if (bytes == null || bytes.isEmpty()) {
                    setError("Görsel okunamadı.")
                    return@launch
                }
                val fileName = "slide_${System.currentTimeMillis()}.jpg"
                repository.uploadSlideImage(bytes, fileName).onSuccess { url ->
                    createSlideWithImage(url)
                }.onFailure { err ->
                    setError(err.message ?: "Yükleme başarısız oldu.")
                }
            } catch (e: Exception) {
                setError(e.message ?: "Yükleme başarısız oldu.")
            }
        }
    }

    /** Pixabay'den seçilen bir görseli slayt olarak ekler (yeniden yükleme gerekmez). */
    fun addSlideFromUrl(imageUrl: String) {
        val current = _state.value as? SlideCreatorUiState.Content ?: return
        if (!current.canAddMore) {
            _state.value = current.copy(error = "En fazla $MAX_GOAL_SLIDES slayt ekleyebilirsin.")
            return
        }
        _state.value = current.copy(isUploading = true, error = null)
        viewModelScope.launch { createSlideWithImage(imageUrl) }
    }

    private suspend fun createSlideWithImage(imageUrl: String) {
        repository.createGoalSlide(goalId = goalId, imageUrl = imageUrl).onSuccess { slide ->
            val latest = _state.value as? SlideCreatorUiState.Content ?: return@onSuccess
            _state.value = latest.copy(
                slides = latest.slides + slide,
                isUploading = false,
                editingSlideId = slide.id // yeni eklenen slaytın altyazı düzenleyicisini hemen aç
            )
        }.onFailure { err ->
            setError(err.message ?: "Slayt oluşturulamadı.")
        }
    }

    private fun setError(message: String) {
        val current = _state.value as? SlideCreatorUiState.Content ?: return
        _state.value = current.copy(isUploading = false, error = message)
    }

    fun clearMessages() {
        val current = _state.value as? SlideCreatorUiState.Content ?: return
        _state.value = current.copy(error = null, infoMessage = null)
    }

    fun startEditing(slideId: String) {
        val current = _state.value as? SlideCreatorUiState.Content ?: return
        _state.value = current.copy(editingSlideId = slideId)
    }

    fun stopEditing() {
        val current = _state.value as? SlideCreatorUiState.Content ?: return
        _state.value = current.copy(editingSlideId = null)
    }

    /**
     * Düzenlenen slaytın metnini/stilini kaydeder. captionX/Y için 3 sabit
     * ön ayar kullanıyoruz (Üst=15, Orta=50, Alt=85) — web'deki serbest
     * sürükleme yerine, Android'de daha güvenilir/basit bir başlangıç.
     */
    fun saveSlideStyle(
        slideId: String,
        caption: String,
        durationSeconds: Int,
        captionFont: String,
        captionColor: String,
        captionYPreset: Float,
        captionSize: Float
    ) {
        val current = _state.value as? SlideCreatorUiState.Content ?: return
        val original = current.slides.find { it.id == slideId } ?: return

        val optimistic = original.copy(
            caption = caption.ifBlank { null },
            durationSeconds = durationSeconds,
            captionFont = captionFont,
            captionColor = captionColor,
            captionX = 50f,
            captionY = captionYPreset,
            captionSize = captionSize
        )
        _state.value = current.copy(
            slides = current.slides.map { if (it.id == slideId) optimistic else it },
            editingSlideId = null
        )

        viewModelScope.launch {
            repository.updateGoalSlide(
                UpdateSlideRequest(
                    slideId = slideId,
                    caption = caption.ifBlank { null },
                    durationSeconds = durationSeconds,
                    captionFont = captionFont,
                    captionColor = captionColor,
                    captionX = 50f,
                    captionY = captionYPreset,
                    captionSize = captionSize
                )
            ).onSuccess { updated ->
                val latest = _state.value as? SlideCreatorUiState.Content ?: return@onSuccess
                _state.value = latest.copy(slides = latest.slides.map { if (it.id == slideId) updated else it })
            }.onFailure { err ->
                val latest = _state.value as? SlideCreatorUiState.Content ?: return@onFailure
                _state.value = latest.copy(
                    slides = latest.slides.map { if (it.id == slideId) original else it },
                    error = err.message ?: "Kaydedilemedi."
                )
            }
        }
    }

    fun deleteSlide(slideId: String) {
        val current = _state.value as? SlideCreatorUiState.Content ?: return
        val removed = current.slides.find { it.id == slideId } ?: return
        val remaining = current.slides.filterNot { it.id == slideId }
        _state.value = current.copy(slides = remaining, editingSlideId = null)

        viewModelScope.launch {
            repository.deleteGoalSlide(slideId).onFailure { err ->
                val latest = _state.value as? SlideCreatorUiState.Content ?: return@onFailure
                _state.value = latest.copy(
                    slides = (latest.slides + removed).sortedBy { it.orderIndex ?: 0 },
                    error = err.message ?: "Silinemedi."
                )
            }
        }
    }

    fun moveSlide(slideId: String, direction: Int) {
        val current = _state.value as? SlideCreatorUiState.Content ?: return
        val index = current.slides.indexOfFirst { it.id == slideId }
        val newIndex = index + direction
        if (index < 0 || newIndex < 0 || newIndex >= current.slides.size) return

        val reordered = current.slides.toMutableList().apply {
            val item = removeAt(index)
            add(newIndex, item)
        }
        _state.value = current.copy(slides = reordered)

        viewModelScope.launch {
            repository.reorderGoalSlides(goalId, reordered.map { it.id }).onFailure {
                loadSlides() // senkron bozulursa güvenli şekilde yeniden yükle
            }
        }
    }

    class Factory(private val goalId: String) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SlideCreatorViewModel(goalId) as T
        }
    }
}
