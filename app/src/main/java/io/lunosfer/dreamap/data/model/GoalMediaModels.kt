package io.lunosfer.dreamap.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GenerateGoalCoverRequest(
    @SerialName("goalId") val goalId: String? = null,
    val title: String? = null,
    val description: String? = null
)

@Serializable
data class GoalCoverResponse(
    val ok: Boolean? = true,
    val url: String? = null,
    @SerialName("coverImageUrl") val coverImageUrl: String? = null,
    val goal: Goal? = null,
    val error: String? = null
)

@Serializable
data class GoalPixabayImageRequest(
    @SerialName("goalId") val goalId: String,
    @SerialName("pixabayId") val pixabayId: Long,
    @SerialName("imageUrl") val imageUrl: String,
    val tags: String = "",
    @SerialName("pixabayUser") val pixabayUser: String = "",
    val width: Int = 1920,
    val height: Int = 1080
)

@Serializable
data class GoalAddImageRequest(
    @SerialName("goalId") val goalId: String,
    @SerialName("imageUrl") val imageUrl: String
)

@Serializable
data class GoalSetCoverRequest(
    @SerialName("goalId") val goalId: String,
    @SerialName("imageUrl") val imageUrl: String
)

@Serializable
data class GoalRemoveImageRequest(
    @SerialName("goalId") val goalId: String,
    @SerialName("imageUrl") val imageUrl: String
)

@Serializable
data class GoalImageResponse(
    val ok: Boolean? = true,
    @SerialName("imageUrl") val imageUrl: String? = null,
    val error: String? = null
)
