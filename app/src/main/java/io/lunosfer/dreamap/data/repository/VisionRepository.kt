package io.lunosfer.dreamap.data.repository

import io.github.jan.supabase.storage.storage
import io.lunosfer.dreamap.data.model.*
import io.lunosfer.dreamap.data.network.NetworkModule

/** "Vizyon" sekmesi: genel keşfet akışı ve vizyon etkileşimleri. */
class VisionRepository {
    private val api = NetworkModule.api

    suspend fun loadFirstPage(): Result<List<Goal>> = runCatching {
        api.getGoalsFeed(mode = "feed", page = 0, status = null).goals
    }

    suspend fun loadHubGoals(status: String): Result<List<Goal>> = runCatching {
        api.getGoalsFeed(mode = "feed", page = 0, status = status).goals
    }

    suspend fun createGoal(request: CreateGoalRequest): Result<Goal> = runCatching {
        val res = api.createGoal(request)
        if (res.goal == null && res.error != null) {
            throw Exception(res.error)
        }
        res.goal ?: throw Exception("Vizyon oluşturulamadı")
    }

    suspend fun updateGoalStatus(goalId: String, status: String, story: String? = null): Result<Goal> = runCatching {
        val res = api.updateGoalStatus(UpdateGoalStatusRequest(goalId, status, story))
        if (res.goal == null && res.error != null) {
            throw Exception(res.error)
        }
        res.goal ?: throw Exception("Durum güncellenemedi")
    }

    suspend fun deleteGoal(goalId: String): Result<Unit> = runCatching {
        val res = api.deleteGoal(DeleteGoalRequest(goalId))
        if (!res.success && !res.ok && res.error != null) {
            throw Exception(res.error)
        }
        Unit
    }

    suspend fun saveGoal(goalId: String): Result<Boolean> = runCatching {
        val res = api.saveGoal(SaveGoalRequest(goalId))
        if (res.error != null) {
            throw Exception(res.error)
        }
        res.saved
    }

    suspend fun giveMana(goalId: String, amount: Int = 1): Result<GiveManaResponse> = runCatching {
        val res = api.giveMana(GiveManaRequest(goalId, amount))
        if (res.error != null) {
            throw Exception(res.error)
        }
        res
    }

    suspend fun removeMana(goalId: String): Result<Unit> = runCatching {
        val res = api.removeMana(DeleteGoalRequest(goalId))
        if (!res.success && !res.ok && res.error != null) {
            throw Exception(res.error)
        }
        Unit
    }

    // --- Vizyon Slaytları (SlidesViewer fallback) ---

    suspend fun loadGoalSlides(goalId: String): Result<GoalSlidesResponse> = runCatching {
        api.getGoalSlides(goalId)
    }

    suspend fun deleteGoalSlide(slideId: String): Result<Unit> = runCatching {
        val res = api.deleteGoalSlide(DeleteSlideRequest(slideId))
        if (!res.success && !res.ok && res.error != null) {
            throw Exception(res.error)
        }
        Unit
    }

    suspend fun toggleSlideSave(slideId: String): Result<Unit> = runCatching {
        val res = api.toggleSlideSave(SaveSlideRequest(slideId))
        if (!res.success && !res.ok && res.error != null) {
            throw Exception(res.error)
        }
        Unit
    }

    // --- Slayt oluşturma/düzenleme ---

    suspend fun uploadSlideImage(byteArray: ByteArray, fileName: String): Result<String> = runCatching {
        val uniquePath = "${java.util.UUID.randomUUID()}_$fileName"
        try {
            val bucket = io.lunosfer.dreamap.supabase.supabaseClient.storage.from("goal-images")
            bucket.upload(uniquePath, byteArray) { upsert = true }
            bucket.publicUrl(uniquePath)
        } catch (e: Exception) {
            try {
                val bucket = io.lunosfer.dreamap.supabase.supabaseClient.storage.from("dreams")
                bucket.upload(uniquePath, byteArray) { upsert = true }
                bucket.publicUrl(uniquePath)
            } catch (_: Exception) {
                throw e
            }
        }
    }

    suspend fun createGoalSlide(
        goalId: String,
        imageUrl: String,
        caption: String? = null,
        durationSeconds: Int? = null
    ): Result<GoalSlide> = runCatching {
        val res = api.createGoalSlide(CreateSlideRequest(goalId, imageUrl, caption, durationSeconds))
        res.slide ?: throw Exception(res.error ?: "Slayt oluşturulamadı")
    }

    suspend fun updateGoalSlide(request: UpdateSlideRequest): Result<GoalSlide> = runCatching {
        val res = api.updateGoalSlide(request)
        res.slide ?: throw Exception(res.error ?: "Slayt güncellenemedi")
    }

    suspend fun reorderGoalSlides(goalId: String, orderedSlideIds: List<String>): Result<Unit> = runCatching {
        val res = api.reorderGoalSlides(ReorderSlidesRequest(goalId, orderedSlideIds))
        if (!res.success && !res.ok && res.error != null) {
            throw Exception(res.error)
        }
        Unit
    }

    suspend fun getGoalComments(goalId: String): Result<List<GoalComment>> = runCatching {
        api.getGoalComments(goalId).comments
    }

    suspend fun createGoalComment(goalId: String, content: String): Result<GoalComment> = runCatching {
        val res = api.createGoalComment(CreateGoalCommentRequest(goalId, content))
        if (res.comment == null && res.error != null) {
            throw Exception(res.error)
        }
        res.comment ?: throw Exception("Yorum eklenemedi")
    }

    suspend fun deleteGoalComment(commentId: String): Result<Unit> = runCatching {
        val res = api.deleteGoalComment(DeleteGoalCommentRequest(commentId))
        if (!res.success && !res.ok && res.error != null) {
            throw Exception(res.error)
        }
        Unit
    }

    // --- Goal Cover & Gallery Media ---

    suspend fun generateGoalCover(goalId: String?, title: String?, description: String?): Result<String> = runCatching {
        val res = api.generateGoalCover(GenerateGoalCoverRequest(goalId, title, description))
        if (res.ok == false && res.error != null) {
            throw Exception(res.error)
        }
        res.coverImageUrl ?: res.url ?: throw Exception("Kapak görseli üretilemedi")
    }

    suspend fun addGoalImageFromPixabay(
        goalId: String,
        pixabayId: Long,
        imageUrl: String,
        tags: String = "",
        pixabayUser: String = "",
        width: Int = 1920,
        height: Int = 1080
    ): Result<String> = runCatching {
        val res = api.addGoalImageFromPixabay(
            GoalPixabayImageRequest(goalId, pixabayId, imageUrl, tags, pixabayUser, width, height)
        )
        if (res.ok == false && res.error != null) {
            throw Exception(res.error)
        }
        res.imageUrl ?: imageUrl
    }

    suspend fun addGoalImage(goalId: String, imageUrl: String): Result<String> = runCatching {
        val res = api.addGoalImage(GoalAddImageRequest(goalId, imageUrl))
        if (res.ok == false && res.error != null) {
            throw Exception(res.error)
        }
        res.imageUrl ?: imageUrl
    }

    suspend fun setGoalCover(goalId: String, imageUrl: String): Result<String> = runCatching {
        val res = api.setGoalCover(GoalSetCoverRequest(goalId, imageUrl))
        if (res.ok == false && res.error != null) {
            throw Exception(res.error)
        }
        res.coverImageUrl ?: imageUrl
    }

    suspend fun removeGoalImage(goalId: String, imageUrl: String): Result<Unit> = runCatching {
        val res = api.removeGoalImage(GoalRemoveImageRequest(goalId, imageUrl))
        if (res.ok == false && res.error != null) {
            throw Exception(res.error)
        }
        Unit
    }


    // --- Daily Compass ---
    suspend fun getDailyCompass(lang: String = "tr"): Result<DailyCompassResponse> = runCatching {
        try {
            api.getDailyCompass(DailyCompassRequest(lang))
        } catch (e: retrofit2.HttpException) {
            if (e.code() == 429) {
                DailyCompassResponse(ok = false, error = "already_used_today")
            } else {
                throw e
            }
        }
    }

    // --- Daily Seeds ---
    suspend fun getDailySeeds(): Result<List<DailySeedItem>> = runCatching {
        api.getDailySeeds().seeds ?: emptyList()
    }

    suspend fun generateDailySeed(goalId: String, lang: String = "tr"): Result<DailySeedItem?> = runCatching {
        val res = api.generateDailySeed(GenerateSeedRequest(goalId, lang))
        if (res.ok == false) {
            throw Exception("Tohum üretilemedi")
        }
        res.seed
    }

    suspend fun completeDailySeed(seedId: String): Result<DailySeedItem?> = runCatching {
        val res = api.completeDailySeed(CompleteSeedRequest(seedId))
        res.seed
    }
}

