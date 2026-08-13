package io.lunosfer.dreamap.data.repository

import io.github.jan.supabase.storage.storage
import io.lunosfer.dreamap.data.model.CreateDiaryInput
import io.lunosfer.dreamap.data.model.DiaryEntry
import io.lunosfer.dreamap.data.model.DiaryListResponse
import io.lunosfer.dreamap.data.model.DiaryRing
import io.lunosfer.dreamap.data.model.DeleteDiaryInput
import io.lunosfer.dreamap.data.model.MarkDiarySeenInput
import io.lunosfer.dreamap.data.network.NetworkModule
import io.lunosfer.dreamap.supabase.supabaseClient
import java.util.UUID

class DiaryRepository {
    private val api = NetworkModule.api

    suspend fun getFeed(): Result<List<DiaryRing>> = runCatching {
        api.getDiaryFeed().rings
    }

    suspend fun getEntriesForUser(userId: String): Result<DiaryListResponse> = runCatching {
        api.getDiaryListForUser(userId)
    }

    suspend fun markSeen(ownerId: String): Result<Unit> = runCatching {
        api.markDiarySeen(MarkDiarySeenInput(ownerId = ownerId))
    }

    suspend fun createEntry(input: CreateDiaryInput): Result<DiaryEntry> = runCatching {
        val res = api.createDiaryEntry(input)
        res.entry ?: throw Exception(res.error ?: "Girdi oluşturulamadı")
    }

    suspend fun deleteEntry(entryId: String): Result<Unit> = runCatching {
        val res = api.deleteDiaryEntry(DeleteDiaryInput(entryId = entryId))
        if (!res.success) {
            throw Exception("Girdi silinemedi")
        }
    }

    suspend fun uploadMediaToStorage(byteArray: ByteArray, fileName: String): Result<String> = runCatching {
        val bucketName = "diary"
        val uniquePath = "${UUID.randomUUID()}_$fileName"
        try {
            val bucket = supabaseClient.storage.from(bucketName)
            bucket.upload(uniquePath, byteArray) {
                upsert = true
            }
            bucket.publicUrl(uniquePath)
        } catch (e: Exception) {
            // Try fallback bucket "public" or "dreams" if "diary" bucket doesn't exist
            try {
                val bucket = supabaseClient.storage.from("dreams")
                bucket.upload(uniquePath, byteArray) {
                    upsert = true
                }
                bucket.publicUrl(uniquePath)
            } catch (_: Exception) {
                throw e
            }
        }
    }
}
