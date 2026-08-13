package io.lunosfer.dreamap.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TranslateRequest(
    val text: String,
    @SerialName("targetLang") val targetLang: String = "tr"
)

@Serializable
data class TranslateResponse(
    @SerialName("translatedText") val translatedText: String? = null,
    val text: String? = null,
    val ok: Boolean? = true,
    val error: String? = null
) {
    val resultText: String get() = translatedText ?: text ?: ""
}
