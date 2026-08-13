package io.lunosfer.dreamap.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// --- 1) Deep Analysis ---

@Serializable
data class GenerateDeepAnalysisRequest(
    @SerialName("dreamId") val dreamId: String
)

@Serializable
data class GenerateDeepAnalysisResponse(
    val ok: Boolean? = true,
    val success: Boolean? = true,
    val analysis: String? = null,
    @SerialName("deep_analysis") val deepAnalysis: String? = null,
    val error: String? = null,
    val message: String? = null
) {
    val resultText: String? get() = analysis ?: deepAnalysis ?: message
}

// --- 2) Daily Compass ---

@Serializable
data class DailyCompassRequest(
    val lang: String = "tr"
)

@Serializable
data class DailyCompassResponse(
    val ok: Boolean? = true,
    val data: DailyCompassData? = null,
    val error: String? = null
)

@Serializable
data class DailyCompassData(
    val reading: String? = null,
    val archetype: String? = null,
    val color: String? = null
)

// --- 3) Daily Seeds ---

@Serializable
data class DailySeedsResponse(
    val seeds: List<DailySeedItem>? = emptyList()
)

@Serializable
data class DailySeedItem(
    val id: String,
    @SerialName("user_id") val userId: String? = null,
    @SerialName("goal_id") val goalId: String,
    @SerialName("seed_date") val seedDate: String? = null,
    @SerialName("seed_text") val seedText: String,
    @SerialName("is_completed") val isCompleted: Boolean = false,
    val goals: SeedGoalInfo? = null
)

@Serializable
data class SeedGoalInfo(
    val id: String? = null,
    val title: String? = null,
    val status: String? = null
)

@Serializable
data class GenerateSeedRequest(
    val goalId: String,
    val lang: String = "tr"
)

@Serializable
data class GenerateSeedResponse(
    val seed: DailySeedItem? = null,
    val ok: Boolean? = true
)

@Serializable
data class CompleteSeedRequest(
    val seedId: String
)

@Serializable
data class CompleteSeedResponse(
    val seed: DailySeedItem? = null,
    val ok: Boolean? = true
)
