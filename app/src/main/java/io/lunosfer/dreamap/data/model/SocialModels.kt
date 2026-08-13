package io.lunosfer.dreamap.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// --- Friends / Social ---

@Serializable
data class Friendship(
    val id: String,
    @SerialName("user_id") val userId: String? = null,
    @SerialName("friend_id") val friendId: String? = null,
    val status: String? = null, // "pending" | "accepted" | "rejected"
    val requester: UserProfile? = null,
    val target: UserProfile? = null
)

@Serializable
data class FriendsListResponse(
    val friendships: List<Friendship> = emptyList()
)

@Serializable
data class FriendRequestInput(
    val userId: String,
    val friendId: String
)

@Serializable
data class FriendRequestResponse(
    val success: Boolean = false,
    val status: String? = null,
    val data: Friendship? = null,
    val error: String? = null,
    val message: String? = null
)

@Serializable
data class FriendRespondInput(
    val friendshipId: String,
    val userId: String,
    val action: String // "accepted" | "rejected"
)

@Serializable
data class FriendRespondResponse(
    val success: Boolean = false,
    val data: Friendship? = null,
    val error: String? = null,
    val message: String? = null
)

@Serializable
data class UserSearchResult(
    val id: String,
    val username: String? = null,
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    val friendshipStatus: String? = null // null | "pending" | "accepted"
) {
    val nameOrFallback: String get() = displayName?.takeIf { it.isNotBlank() }
        ?: username?.takeIf { it.isNotBlank() }
        ?: "?"
}

@Serializable
data class UserSearchResponse(
    val users: List<UserSearchResult> = emptyList()
)

// --- Notifications ---

@Serializable
data class AppNotification(
    val id: String,
    val type: String, // "new_follower", "friend_request", "analysis_ready", "analysis_failed", etc.
    @SerialName("actor_id") val actorId: String? = null,
    @SerialName("dream_id") val dreamId: Long? = null,
    @SerialName("is_read") val isRead: Boolean = false,
    @SerialName("created_at") val createdAt: String? = null,
    val actor: UserProfile? = null
)

@Serializable
data class NotificationsResponse(
    val notifications: List<AppNotification> = emptyList(),
    val unreadCount: Int = 0
)

@Serializable
data class MarkNotificationReadInput(
    @SerialName("notificationId") val notificationId: String? = null
)

@Serializable
data class MarkNotificationReadResponse(
    val success: Boolean = false,
    val error: String? = null
)

// --- Public Profile ---

@Serializable
data class PublicProfileData(
    val id: String,
    val username: String? = null,
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    val bio: String? = null
) {
    val nameOrFallback: String get() = displayName?.takeIf { it.isNotBlank() }
        ?: username?.takeIf { it.isNotBlank() }
        ?: "?"
}

@Serializable
data class PublicProfileResponse(
    val profile: PublicProfileData? = null,
    val dreams: List<Dream> = emptyList(),
    val hasMore: Boolean = false,
    val friendshipStatus: String? = null, // null | "pending" | "accepted"
    val followsViewer: Boolean = false,
    val isSelf: Boolean = false,
    val error: String? = null
)
