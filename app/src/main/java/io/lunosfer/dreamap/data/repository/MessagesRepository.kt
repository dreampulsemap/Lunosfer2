package io.lunosfer.dreamap.data.repository

import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.storage.storage
import io.lunosfer.dreamap.BuildConfig
import io.lunosfer.dreamap.data.model.Conversation
import io.lunosfer.dreamap.data.model.Message
import io.lunosfer.dreamap.data.model.SendMessageRequest
import io.lunosfer.dreamap.data.model.ThreadResponse
import io.lunosfer.dreamap.data.network.NetworkModule
import io.lunosfer.dreamap.supabase.supabaseClient
import retrofit2.HttpException
import java.util.Locale
import java.util.UUID

class MessagesRepository {
    private val api = NetworkModule.api

    private val ATTACHMENT_BUCKET = "message-attachments"

    suspend fun loadConversations(): Result<List<Conversation>> = runCatching {
        api.getConversations().conversations
    }

    /**
     * İlk yükleme (before=null): en son 50 mesaj, thread.js içinde zaten
     * eskiden-yeniye çevrilmiş olarak döner. otherUser bilgisini de aynı
     * yanıtta taşııyoruz ki ekran başlığı için ayrı bir profil çağrısına
     * gerek kalmasın.
     */
    suspend fun loadThread(otherUserId: String): Result<ThreadResponse> = runCatching {
        api.getThread(otherUserId = otherUserId, before = null)
    }

    /** "Daha eski mesajları yükle" — en eski görünen mesajın created_at'i before olarak verilir. */
    suspend fun loadOlderMessages(otherUserId: String, before: String): Result<ThreadResponse> = runCatching {
        api.getThread(otherUserId = otherUserId, before = before)
    }

    /**
     * Cihazdan seçilen bir dosyayı (content:// URI) Supabase Storage'a
     * yükler ve gerçek, public URL'i döner. Path deseni ({userId}/{uuid}-
     * {orijinalAd}) mevcut veritabanındaki gerçek kayıtlardan doğrulandı;
     * bu prefix send.js'deki isOwnAttachment kontrolünün beklediği
     * desenle birebir aynı.
     */
    suspend fun uploadAttachment(
        context: android.content.Context,
        uri: android.net.Uri,
        mimeType: String,
        fileName: String
    ): Result<String> = runCatching {
        val userId = supabaseClient.auth.currentUserOrNull()?.id
            ?: throw IllegalStateException("Yüklemek için giriş yapmış olmalısınız.")

        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw IllegalStateException("Dosya okunamadı.")

        val storagePath = "$userId/${UUID.randomUUID()}-$fileName"

        supabaseClient.storage.from(ATTACHMENT_BUCKET).upload(storagePath, bytes) {
            upsert = false
        }

        supabaseClient.storage.from(ATTACHMENT_BUCKET).publicUrl(storagePath)
    }

    suspend fun sendMessage(
        recipientId: String,
        content: String?,
        attachmentUrl: String? = null,
        attachmentType: String? = null,
        attachmentName: String? = null,
        attachmentMime: String? = null,
        attachmentSize: Long? = null
    ): Result<Message> = runCatching {
        val lang = Locale.getDefault().language
        api.sendMessage(
            SendMessageRequest(
                recipientId = recipientId,
                content = content,
                lang = lang,
                attachmentUrl = attachmentUrl,
                attachmentType = attachmentType,
                attachmentName = attachmentName,
                attachmentMime = attachmentMime,
                attachmentSize = attachmentSize
            )
        ).message
    }

    suspend fun reactMessage(messageId: String, reaction: String): Result<Unit> = runCatching {
        api.reactMessage(io.lunosfer.dreamap.data.model.ReactMessageRequest(messageId, reaction))
    }
}

