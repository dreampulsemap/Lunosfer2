package io.lunosfer.dreamap.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// --- Like DTOs ---

@Serializable
data class LikeRequest(
    val dreamId: Long,
    val userId: String
)

@Serializable
data class LikeResponse(
    val success: Boolean = false,
    val liked: Boolean = false,
    val count: Int? = null,
    val error: String? = null
)

// --- Comment DTOs ---

@Serializable
data class DreamComment(
    val id: Long,
    val content: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("user_id") val userId: String,
    @SerialName("user_profiles") val userProfile: UserProfile? = null
)

@Serializable
data class CommentsResponse(
    val comments: List<DreamComment> = emptyList()
)

@Serializable
data class CreateCommentRequest(
    val dreamId: Long,
    val userId: String,
    val content: String
)

@Serializable
data class CreateCommentResponse(
    val success: Boolean = false,
    val comment: DreamComment? = null,
    val error: String? = null
)

@Serializable
data class DeleteCommentRequest(
    val commentId: Long,
    val userId: String
)

// --- Update Dream DTO ---

@Serializable
data class UpdateDreamRequest(
    val dreamId: Long,
    val userId: String,
    val content: String? = null,
    @SerialName("location_name") val locationName: String? = null,
    val visibility: String? = null,
    @SerialName("map_detail") val mapDetail: String? = null,
    @SerialName("in_feed") val inFeed: Boolean? = null,
    val tags: List<String>? = null,
    @SerialName("ai_image_url") val aiImageUrl: String? = null,
    @SerialName("image_source") val imageSource: String? = null,
    @SerialName("image_width") val imageWidth: Int? = null,
    @SerialName("image_height") val imageHeight: Int? = null
)

// --- Delete Dream DTO ---

@Serializable
data class DeleteDreamRequest(
    val dreamId: Long,
    val userId: String,
    val softDelete: Boolean = false
)

// --- Boost Dream DTOs ---

@Serializable
data class BoostDreamRequest(
    val dreamId: Long
)

@Serializable
data class BoostDreamResponse(
    val ok: Boolean = false,
    @SerialName("aurasLeft") val aurasLeft: Int? = null,
    val error: String? = null
)

// --- Bounty DTOs ---

@Serializable
data class AddBountyRequest(
    val dreamId: Long,
    val bountyAmount: Int
)

@Serializable
data class AddBountyResponse(
    val ok: Boolean = false,
    @SerialName("aurasLeft") val aurasLeft: Int? = null,
    @SerialName("newBounty") val newBounty: Int? = null,
    val error: String? = null
)

// --- Generic Response DTO ---

@Serializable
data class GenericSuccessResponse(
    val success: Boolean = false,
    val ok: Boolean = false,
    val error: String? = null
)
