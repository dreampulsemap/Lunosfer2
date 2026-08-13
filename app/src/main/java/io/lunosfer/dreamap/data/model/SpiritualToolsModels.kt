package io.lunosfer.dreamap.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// --- Mental Wall ---
@Serializable
data class MentalWallRequest(
    val lang: String = "tr"
)

@Serializable
data class MentalWallResponse(
    val ok: Boolean? = true,
    val image: String? = null,
    @SerialName("image_url") val imageUrl: String? = null,
    val url: String? = null,
    val narrative: String? = null,
    val summary: String? = null,
    val archetypes: List<String>? = emptyList(),
    val error: String? = null
) {
    val displayImage: String? get() = image ?: imageUrl ?: url
    val displayText: String? get() = narrative ?: summary
}

// --- Psyche Map ---
@Serializable
data class PsycheMapResponse(
    val ok: Boolean? = true,
    @SerialName("dominant_archetype") val dominantArchetype: String? = null,
    @SerialName("psychic_score") val psychicScore: Int? = null,
    val summary: String? = null,
    val archetypes: List<PsycheArchetypeItem>? = emptyList(),
    val nodes: List<PsycheNode>? = emptyList(),
    val error: String? = null
)

@Serializable
data class PsycheArchetypeItem(
    val name: String,
    val percentage: Int? = 0,
    val count: Int? = 0,
    val description: String? = null
)

@Serializable
data class PsycheNode(
    val id: String,
    val label: String,
    val type: String? = null,
    val weight: Float? = 1f
)

// --- Prophet ---
@Serializable
data class ProphetRequest(
    val question: String? = null,
    val lang: String = "tr"
)

@Serializable
data class ProphetResponse(
    val ok: Boolean? = true,
    val prophecy: String? = null,
    val answer: String? = null,
    val card: String? = null,
    val guidance: String? = null,
    val error: String? = null
) {
    val resultText: String? get() = prophecy ?: answer ?: guidance
}
