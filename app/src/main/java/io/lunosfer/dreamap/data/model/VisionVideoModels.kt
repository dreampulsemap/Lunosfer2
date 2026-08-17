package io.lunosfer.dreamap.data.model

import kotlinx.serialization.Serializable

// pages/api/goals/save-vision-video.js — reels editöründen export edilip
// Storage'a yüklenen video URL'ini goals.vision_video_url'e yazar.
@Serializable
data class SaveVisionVideoRequest(
    val goalId: String,
    val videoUrl: String,
)

@Serializable
data class SaveVisionVideoResponse(
    val goal: Goal,
    val visionVideoUrl: String? = null,
)
