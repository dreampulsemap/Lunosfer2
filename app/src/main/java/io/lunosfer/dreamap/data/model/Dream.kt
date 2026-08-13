package io.lunosfer.dreamap.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * dreams tablosu satırı. Alan isimleri pages/api/submit-dream.js (insert payload)
 * ve pages/api/home-feed.js / explore/feed.js (select + owner attach) ile eşleşir.
 * Web tarafında henüz kullanılmayan/gösterilmeyen sütunlar (ör. latitude,
 * map_detail) buraya alınmadı — sadece kart render için gerekenler.
 */
@Serializable
data class Dream(
    val id: Long,
    @SerialName("user_id") val userId: String,
    val content: String,
    @SerialName("ai_title") val aiTitle: String? = null,
    @SerialName("ai_image_url") val aiImageUrl: String? = null,
    @SerialName("ai_archetypes") val aiArchetypes: List<String>? = null,
    @SerialName("image_status") val imageStatus: String? = null,
    @SerialName("user_selected_sentiment") val userSelectedSentiment: String? = null,
    @SerialName("dream_date") val dreamDate: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("likes_count") val likesCount: Int? = 0,
    @SerialName("comments_count") val commentsCount: Int? = 0,
    val visibility: String? = null,
    // home-feed.js JOIN sonucunda ekleniyor; explore/feed.js'de de owner attachOwners() ile geliyor.
    val owner: UserProfile? = null,
    // home-feed.js her item'a feed_type: 'dream' | 'vision' ekliyor, tek listede ayırt etmek için.
    @SerialName("feed_type") val feedType: String? = "dream",
    @SerialName("ai_jungian_analysis") val aiJungianAnalysis: AiJungianAnalysis? = null,
    @SerialName("ai_sentiment") val aiSentiment: String? = null,
    @SerialName("premium_deep_analysis") val premiumDeepAnalysis: JsonElement? = null,
    @SerialName("premium_deep_analysis_status") val premiumDeepAnalysisStatus: String? = null
) {
    val displayTitle: String get() = aiTitle?.takeIf { it.isNotBlank() } ?: content.take(60)
}
