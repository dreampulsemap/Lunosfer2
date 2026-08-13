package io.lunosfer.dreamap.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * user_profiles tablosundan owner/otherUser alanları için ortak model.
 * pages/api/home-feed.js, explore/feed.js, goals/list.js, messages/endpoints
 * hepsi bu şekildeki { id, username, display_name, avatar_url } nesnesini döner.
 */
@Serializable
data class UserProfile(
    val id: String,
    val username: String? = null,
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null
) {
    /** UI'da gösterilecek isim: display_name yoksa username'e, o da yoksa "?"e düşer. */
    val nameOrFallback: String get() = displayName?.takeIf { it.isNotBlank() }
        ?: username?.takeIf { it.isNotBlank() }
        ?: "?"
}
