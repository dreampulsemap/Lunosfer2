package io.lunosfer.dreamap.ui.viewmodel

/** Home/Explore/Vision/Messages ekranlarının hepsinde tekrar eden basit yükleme durumu. */
sealed class UiState<out T> {
    object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String) : UiState<Nothing>()
}
