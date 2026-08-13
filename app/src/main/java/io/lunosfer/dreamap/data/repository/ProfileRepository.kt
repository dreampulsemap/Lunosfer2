package io.lunosfer.dreamap.data.repository

import io.github.jan.supabase.postgrest.postgrest
import io.lunosfer.dreamap.data.model.FullUserProfile
import io.lunosfer.dreamap.data.model.PremiumStatusResponse
import io.lunosfer.dreamap.data.model.UpdateProfileRequest
import io.lunosfer.dreamap.data.model.UpdateProfileResponse
import io.lunosfer.dreamap.data.network.NetworkModule
import io.lunosfer.dreamap.supabase.supabaseClient
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import retrofit2.HttpException

class ProfileRepository {
    private val api = NetworkModule.api
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun getUserProfile(userId: String): Result<FullUserProfile> = runCatching {
        try {
            val list = supabaseClient.postgrest["user_profiles"]
                .select { filter { eq("id", userId) } }
                .decodeList<FullUserProfile>()
            if (list.isNotEmpty()) {
                return@runCatching list.first()
            }
        } catch (_: Exception) {}

        // Fallback to public-profile API
        val res = api.getPublicProfile(userId = userId, page = 0)
        val p = res.profile
            ?: throw Exception("Profil bulunamadı")
        FullUserProfile(
            id = p.id,
            username = p.username,
            displayName = p.displayName,
            avatarUrl = p.avatarUrl,
            bio = p.bio
        )
    }

    suspend fun updateProfile(request: UpdateProfileRequest): Result<FullUserProfile> = runCatching {
        try {
            val res = api.updateProfile(request)
            if (!res.success) {
                throw Exception(res.error ?: res.message ?: "Profil güncellenemedi")
            }
            res.profile ?: FullUserProfile(id = request.userId)
        } catch (e: HttpException) {
            val parsedError = parseHttpError(e)
            throw Exception(parsedError)
        }
    }

    suspend fun getPremiumStatus(): Result<PremiumStatusResponse> = runCatching {
        api.getPremiumStatus()
    }

    private fun parseHttpError(e: HttpException): String {
        val errorBody = e.response()?.errorBody()?.string()
        if (!errorBody.isNullOrBlank()) {
            try {
                val obj = json.parseToJsonElement(errorBody).jsonObject
                val err = obj["error"]?.jsonPrimitive?.content
                    ?: obj["message"]?.jsonPrimitive?.content
                if (!err.isNullOrBlank()) {
                    return err
                }
            } catch (_: Exception) {}
        }
        return when (e.code()) {
            409 -> "Bu kullanıcı adı zaten başka bir kullanıcı tarafından kullanılıyor."
            400 -> "Profil bilgileri geçersiz. Lütfen kontrol edip tekrar deneyin."
            else -> "Profil güncellenirken bir hata oluştu (${e.code()})."
        }
    }
}
