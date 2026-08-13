package io.lunosfer.dreamap.data.repository

import io.lunosfer.dreamap.data.model.*
import io.lunosfer.dreamap.data.network.NetworkModule

class DreamRepository {
    private val api = NetworkModule.api

    suspend fun getDream(id: Long): Result<DreamDetail> = runCatching {
        api.getDream(id).dream
    }

    suspend fun analyzeDream(dreamId: Long, content: String, lang: String): Result<Unit> = runCatching {
        api.analyzeDream(AnalyzeDreamRequest(dreamId, content, lang))
    }

    suspend fun likeDream(dreamId: Long, userId: String): Result<LikeResponse> = runCatching {
        try {
            api.likeDream(LikeRequest(dreamId, userId))
        } catch (e: Exception) {
            if (e.message?.contains("Already liked", ignoreCase = true) == true) {
                LikeResponse(success = true, liked = true)
            } else {
                throw e
            }
        }
    }

    suspend fun unlikeDream(dreamId: Long, userId: String): Result<LikeResponse> = runCatching {
        api.unlikeDream(LikeRequest(dreamId, userId))
    }

    suspend fun getComments(dreamId: Long): Result<List<DreamComment>> = runCatching {
        api.getComments(dreamId).comments
    }

    suspend fun createComment(dreamId: Long, userId: String, content: String): Result<DreamComment?> = runCatching {
        val res = api.createComment(CreateCommentRequest(dreamId, userId, content))
        if (!res.success && res.error != null) {
            throw Exception(res.error)
        }
        res.comment
    }

    suspend fun deleteComment(commentId: Long, userId: String): Result<Unit> = runCatching {
        val res = api.deleteComment(DeleteCommentRequest(commentId, userId))
        if (!res.success && !res.ok && res.error != null) {
            throw Exception(res.error)
        }
        Unit
    }

    suspend fun updateDream(request: UpdateDreamRequest): Result<Unit> = runCatching {
        val res = api.updateDream(request)
        if (!res.success && !res.ok && res.error != null) {
            throw Exception(res.error)
        }
        Unit
    }

    suspend fun deleteDream(dreamId: Long, userId: String, softDelete: Boolean = false): Result<Unit> = runCatching {
        val res = api.deleteDream(DeleteDreamRequest(dreamId, userId, softDelete))
        if (!res.success && !res.ok && res.error != null) {
            throw Exception(res.error)
        }
        Unit
    }

    suspend fun boostDream(dreamId: Long): Result<BoostDreamResponse> = runCatching {
        val res = api.boostDream(BoostDreamRequest(dreamId))
        if (!res.ok && res.error != null) {
            throw Exception(res.error)
        }
        res
    }

    suspend fun addBounty(dreamId: Long, bountyAmount: Int): Result<AddBountyResponse> = runCatching {
        val res = api.addBounty(AddBountyRequest(dreamId, bountyAmount))
        if (!res.ok && res.error != null) {
            throw Exception(res.error)
        }
        res
    }

    suspend fun generateDeepAnalysis(dreamId: Long): Result<GenerateDeepAnalysisResponse> = runCatching {
        val res = api.generateDeepAnalysis(GenerateDeepAnalysisRequest(dreamId = dreamId.toString()))
        if (res.ok == false && res.error != null) {
            throw Exception(res.error)
        }
        res
    }
}
