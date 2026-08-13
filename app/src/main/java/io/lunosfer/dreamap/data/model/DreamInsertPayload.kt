package io.lunosfer.dreamap.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DreamInsertPayload(
    @SerialName("user_id") val userId: String,
    val content: String,
    @SerialName("location_name") val locationName: String,
    @SerialName("in_feed") val inFeed: Boolean,
    val visibility: String,
    @SerialName("user_selected_sentiment") val userSelectedSentiment: String,
    @SerialName("dream_date") val dreamDate: String,
    @SerialName("original_language") val originalLanguage: String,
    val tags: List<String>,
    @SerialName("ai_image_url") val aiImageUrl: String? = null,
    @SerialName("image_source") val imageSource: String? = null,
    @SerialName("image_width") val imageWidth: Int? = null,
    @SerialName("image_height") val imageHeight: Int? = null,
    @SerialName("analysis_status") val analysisStatus: String = "processing"
)
