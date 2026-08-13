package io.lunosfer.dreamap.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * goals tablosu satırı ("Vizyon" = Vision Board hedefi). Alanlar
 * pages/api/goals/create.js (insert payload) ve pages/api/goals/list.js
 * (select + join'ler) ile eşleşir. GoalCard.jsx'te render edilen alanların
 * hepsi burada: title, cover_image_url, status, completion_percentage,
 * believers_count, has_reacted, has_saved, slide_count, owner.
 */
@Serializable
data class Goal(
    val id: String,
    @SerialName("user_id") val userId: String,
    val title: String,
    val description: String? = null,
    @SerialName("cover_image_url") val coverImageUrl: String? = null,
    @SerialName("cover_image_source") val coverImageSource: String? = null,
    val status: String = "active",
    val visibility: String = "public",
    @SerialName("completion_percentage") val completionPercentage: Int? = 0,
    @SerialName("believers_count") val believersCount: Int? = 0,
    @SerialName("has_reacted") val hasReacted: Boolean? = false,
    @SerialName("has_saved") val hasSaved: Boolean? = false,
    @SerialName("slide_count") val slideCount: Int? = 0,
    @SerialName("target_date") val targetDate: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("micro_goals") val microGoals: List<MicroGoal>? = null,
    val owner: UserProfile? = null,
    @SerialName("feed_type") val feedType: String? = "vision"
)

@Serializable
data class MicroGoal(
    val id: String,
    val title: String,
    @SerialName("is_completed") val isCompleted: Boolean = false,
    @SerialName("order_index") val orderIndex: Int? = null
)

@Serializable
data class GoalsListResponse(
    val goals: List<Goal>,
    val page: Int,
    val hasMore: Boolean
)
