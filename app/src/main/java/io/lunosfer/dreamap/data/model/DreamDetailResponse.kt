package io.lunosfer.dreamap.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DreamDetailResponse(
    val dream: DreamDetail
)

@Serializable
data class DreamDetail(
    val id: Long,
    @SerialName("user_id") val userId: String,
    val content: String,
    @SerialName("location_name") val locationName: String? = null,
    val visibility: String,
    @SerialName("in_feed") val inFeed: Boolean,
    @SerialName("user_selected_sentiment") val userSelectedSentiment: String? = null,
    @SerialName("dream_date") val dreamDate: String,
    @SerialName("original_language") val originalLanguage: String? = null,
    val tags: List<String>? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("analysis_status") val analysisStatus: String? = null,
    @SerialName("analysis_error") val analysisError: String? = null,
    @SerialName("ai_jungian_analysis") val aiJungianAnalysis: AiJungianAnalysis? = null,
    @SerialName("cover_image_url") val coverImageUrl: String? = null,
    @SerialName("ai_image_url") val aiImageUrl: String? = null,
    @SerialName("image_url") val imageUrl: String? = null,
    @SerialName("likes_count") val likesCount: Int = 0,
    @SerialName("comments_count") val commentsCount: Int = 0,
    @SerialName("is_liked") val isLiked: Boolean = false,
    val liked: Boolean = false,
    @SerialName("bounty_amount") val bountyAmount: Int = 0,
    val bounty: Int = 0,
    val owner: UserProfile? = null
) {
    val displayImageUrl: String? get() = coverImageUrl?.takeIf { it.isNotBlank() }
        ?: aiImageUrl?.takeIf { it.isNotBlank() }
        ?: imageUrl?.takeIf { it.isNotBlank() }

    val effectiveIsLiked: Boolean get() = isLiked || liked
    val effectiveBounty: Int get() = if (bounty > 0) bounty else bountyAmount
}

@Serializable
data class AiJungianAnalysis(
    val title: Map<String, String>? = null,
    val summary: Map<String, String>? = null,
    val motiv: Map<String, String>? = null,
    val sentiment: String? = null,
    val archetypes: List<String>? = null
)
