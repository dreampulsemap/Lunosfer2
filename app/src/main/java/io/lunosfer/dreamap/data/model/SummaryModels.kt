package io.lunosfer.dreamap.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GenerateSummaryRequest(
    @SerialName("periodType") val periodType: String
)

@Serializable
data class SummaryResponse(
    val summary: SummaryData? = null,
    val ok: Boolean? = true,
    val error: String? = null
)

@Serializable
data class SummaryData(
    val id: String? = null,
    @SerialName("periodType") val periodType: String? = null,
    @SerialName("period_type") val periodTypeSnake: String? = null,
    @SerialName("periodStart") val periodStart: String? = null,
    @SerialName("periodEnd") val periodEnd: String? = null,
    @SerialName("summaryText") val summaryText: String? = null,
    @SerialName("summary_text") val summaryTextSnake: String? = null,
    @SerialName("dreamCount") val dreamCount: Int? = null,
    @SerialName("dominantArchetypes") val dominantArchetypes: List<String>? = emptyList(),
    @SerialName("dominantSentiment") val dominantSentiment: String? = null,
    @SerialName("createdAt") val createdAt: String? = null
) {
    val type: String get() = periodType ?: periodTypeSnake ?: "weekly"
    val text: String get() = summaryText ?: summaryTextSnake ?: ""
}
