package io.lunosfer.dreamap.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.lunosfer.dreamap.data.model.Message
import io.lunosfer.dreamap.data.model.UserProfile
import io.lunosfer.dreamap.data.repository.MessagesRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Bir thread ekranının tüm durumu. Home/Explore/Vision'daki basit
 * UiState<T> burada yeterli değil çünkü aynı anda birden fazla bağımsız
 * durum var: mesaj listesinin kendisi, gönderim sırasında input'un
 * durumu, ve "daha eski yükle" durumu. Bunları tek bir data class'ta
 * tutmak, ekranın her zaman tutarlı bir anlık görüntüsünü okumasını
 * sağlıyor (partial UiState kombinasyonlarıyla uğraşmak yerine).
 */
data class ThreadUiState(
    val isInitialLoading: Boolean = true,
    val loadError: String? = null,
    val otherUser: UserProfile? = null,
    val messages: List<Message> = emptyList(),
    val hasMoreOlder: Boolean = false,
    val isLoadingOlder: Boolean = false,
    val isSending: Boolean = false,
    val isUploadingAttachment: Boolean = false,
    val sendError: String? = null
)

class ThreadViewModel(
    private val otherUserId: String,
    private val currentUserId: String?,
    private val repository: MessagesRepository = MessagesRepository()
) : ViewModel() {

    private val _state = MutableStateFlow(ThreadUiState())
    val state: StateFlow<ThreadUiState> = _state.asStateFlow()

    init {
        loadInitial()
        startPolling()
    }

    fun retry() = loadInitial()

    private fun loadInitial() {
        _state.value = _state.value.copy(isInitialLoading = true, loadError = null)
        viewModelScope.launch {
            repository.loadThread(otherUserId)
                .onSuccess { response ->
                    _state.value = _state.value.copy(
                        isInitialLoading = false,
                        otherUser = response.otherUser,
                        messages = response.messages,
                        hasMoreOlder = response.hasMore
                    )
                }
                .onFailure { error ->
                    _state.value = _state.value.copy(
                        isInitialLoading = false,
                        loadError = error.message ?: "Bilinmeyen hata"
                    )
                }
        }
    }

    fun loadOlder() {
        val current = _state.value
        val oldest = current.messages.firstOrNull() ?: return
        if (current.isLoadingOlder || !current.hasMoreOlder) return

        _state.value = current.copy(isLoadingOlder = true)
        viewModelScope.launch {
            repository.loadOlderMessages(otherUserId, before = oldest.createdAt)
                .onSuccess { response ->
                    _state.value = _state.value.copy(
                        isLoadingOlder = false,
                        // Eski sayfa + mevcut liste: id bazlı dedupe, olası
                        // sınır çakışmalarına karşı (aynı created_at'e sahip
                        // iki mesaj varsa before/after sınırında tekrar gelebilir).
                        messages = (response.messages + _state.value.messages)
                            .distinctBy { it.id },
                        hasMoreOlder = response.hasMore
                    )
                }
                .onFailure {
                    // Sessizce vazgeç — kullanıcı zaten mesajları görüyor,
                    // "daha eski yükle" başarısız oldu diye tüm ekranı
                    // hataya çevirmenin bir faydası yok. Tekrar kaydırınca
                    // yeniden denenir çünkü isLoadingOlder false'a döner.
                    _state.value = _state.value.copy(isLoadingOlder = false)
                }
        }
    }

    /**
     * content: metin (opsiyonel). localUri: cihazdan seçilen, henüz
     * yüklenmemiş bir dosya (varsa önce Storage'a yüklenir, gerçek URL'i
     * elde edilir). directUrl: zaten hazır bir URL (örn. "Medya Bağlantısı
     * Ekle" seçeneğiyle girilen link) — bu durumda upload adımı atlanır.
     */
    fun sendMessage(
        content: String?,
        localUri: android.net.Uri? = null,
        directUrl: String? = null,
        attachmentType: String? = null,
        attachmentName: String? = null,
        attachmentMime: String? = null,
        attachmentSize: Long? = null,
        appContext: android.content.Context? = null
    ) {
        val trimmed = content?.trim()
        if ((trimmed == null || trimmed.isEmpty()) && localUri == null && directUrl == null) return
        if (_state.value.isSending || _state.value.isUploadingAttachment) return

        _state.value = _state.value.copy(isSending = true, sendError = null)
        viewModelScope.launch {
            val resolvedUrl: String? = if (localUri != null && appContext != null) {
                _state.value = _state.value.copy(isUploadingAttachment = true)
                val uploadResult = repository.uploadAttachment(
                    context = appContext,
                    uri = localUri,
                    mimeType = attachmentMime ?: "application/octet-stream",
                    fileName = attachmentName ?: "dosya"
                )
                _state.value = _state.value.copy(isUploadingAttachment = false)

                uploadResult.getOrElse { error ->
                    _state.value = _state.value.copy(
                        isSending = false,
                        sendError = "Dosya yüklenemedi: ${error.message ?: "bilinmeyen hata"}"
                    )
                    return@launch
                }
            } else {
                directUrl
            }

            repository.sendMessage(
                recipientId = otherUserId,
                content = if (trimmed.isNullOrEmpty()) null else trimmed,
                attachmentUrl = resolvedUrl,
                attachmentType = attachmentType,
                attachmentName = attachmentName,
                attachmentMime = attachmentMime,
                attachmentSize = attachmentSize
            )
                .onSuccess { sentMessage ->
                    _state.value = _state.value.copy(
                        isSending = false,
                        messages = _state.value.messages + sentMessage
                    )
                }
                .onFailure { error ->
                    _state.value = _state.value.copy(
                        isSending = false,
                        sendError = error.message ?: "Mesaj gönderilemedi"
                    )
                }
        }
    }

    fun reactMessage(messageId: String, reaction: String) {
        viewModelScope.launch {
            // Optimistic update
            val currentMessages = _state.value.messages
            val updatedMessages = currentMessages.map { 
                if (it.id == messageId) it.copy(reaction = reaction) else it
            }
            _state.value = _state.value.copy(messages = updatedMessages)

            repository.reactMessage(messageId, reaction)
                .onFailure {
                    // Revert on failure
                    _state.value = _state.value.copy(messages = currentMessages)
                }
        }
    }

    fun dismissSendError() {
        _state.value = _state.value.copy(sendError = null)
    }

    fun isOwnMessage(message: Message): Boolean = message.senderId == currentUserId

    /**
     * Basit poll: her 4 saniyede bir tüm thread'i yeniden çeker ve id
     * bazlı diff alır. thread.js'nin desteklediği after= (yalnızca yeni
     * mesajları getiren) varyantı yerine bilinçli olarak bunu seçtik —
     * ilk sürüm için mevcut loadThread çağrısını olduğu gibi kullanmak,
     * ayrı bir "after" kod yolu eklemekten daha az karmaşıklık taşıyor.
     * Maliyeti: her pollde en fazla 50 mesajlık bir sorgu (SCAN_LIMIT'e
     * kıyasla küçük). Gerekirse sonra after= tabanlı hafif polling'e
     * geçilebilir.
     *
     * Not: thread.js her GET çağrısında karşı taraftan gelen okunmamış
     * mesajları otomatik is_read=true yapıyor — yani her poll, zaten
     * okunmuş mesajlar için de ucuz bir WHERE is_read=false UPDATE
     * sorgusu tetikler. Kabul edilebilir bir maliyet (index'li, boş
     * sonuç), ama bilinçli bir trade-off olduğunu not ediyoruz.
     */
    private fun startPolling() {
        viewModelScope.launch {
            while (isActive) {
                delay(4000)
                if (_state.value.isSending) continue // kendi gönderimimizin üstüne yazmayalım
                repository.loadThread(otherUserId).onSuccess { response ->
                    val current = _state.value
                    // response önce: aynı id'de sunucudan gelen taze veri
                    // (örn. is_read değişmiş olabilir) eskisinin üzerine
                    // yazsın — distinctBy ilk gördüğünü tutar.
                    val merged = (response.messages + current.messages).distinctBy { it.id }
                    if (merged.size != current.messages.size || merged != current.messages) {
                        _state.value = current.copy(messages = merged.sortedBy { it.createdAt })
                    }
                }
                // Poll hatalarını sessizce yut — arka planda deniyoruz,
                // kullanıcının o an baktığı ekranı bir ağ hatası yüzünden
                // bozmanın anlamı yok.
            }
        }
    }

    /**
     * Diğer ViewModel'lerin aksine ThreadViewModel iki runtime parametresi
     * (otherUserId, currentUserId) alıyor, bu yüzden viewModel()'in
     * varsayılan parametresiz constructor beklentisiyle çalışmıyor.
     * Bu factory, ThreadScreen'de viewModel(factory = ...) ile kullanılır.
     */
    class Factory(
        private val otherUserId: String,
        private val currentUserId: String?
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
            return ThreadViewModel(otherUserId, currentUserId) as T
        }
    }
}
