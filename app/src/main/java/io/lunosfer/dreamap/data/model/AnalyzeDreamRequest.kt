package io.lunosfer.dreamap.data.model

import kotlinx.serialization.Serializable

@Serializable
data class AnalyzeDreamRequest(
    val dreamId: Long,
    val content: String,
    val lang: String
)
