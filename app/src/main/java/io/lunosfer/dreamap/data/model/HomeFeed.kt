package io.lunosfer.dreamap.data.model

import kotlinx.serialization.Serializable

/**
 * pages/api/home-feed.js "items" alanını tek bir karışık dream+goal listesi
 * olarak dönüyor (feed_type ile ayırt ediliyor). Moshi'de tek adapter'la
 * polymorphic parse etmek yerine, home-feed'i type=dreams ve type=visions
 * olarak AYRI iki çağrıda çekip burada birleştiriyoruz (bkz. HomeFeedRepository) —
 * API zaten ?type= parametresini destekliyor, bu da iki temiz tip-güvenli
 * response'a izin veriyor ve web'deki polymorphic union'ı Kotlin tarafında
 * taklit etme ihtiyacını ortadan kaldırıyor.
 */
@Serializable
data class DreamsFeedResponse(
    val items: List<Dream>,
    val nextDreamsBefore: String? = null,
    val hasMore: Boolean = false
)

@Serializable
data class VisionsFeedResponse(
    val items: List<Goal>,
    val nextVisionsBefore: String? = null,
    val hasMore: Boolean = false
)

/** UI'da tek bir kronolojik listede göstermek için ortak sarmalayıcı. */
sealed class FeedItem {
    abstract val createdAt: String
    data class DreamItem(val dream: Dream) : FeedItem() {
        override val createdAt get() = dream.createdAt
    }
    data class VisionItem(val goal: Goal) : FeedItem() {
        override val createdAt get() = goal.createdAt
    }
}
