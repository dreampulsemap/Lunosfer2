package io.lunosfer.dreamap.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DiaryRing(
    val userId: String = "",
    val username: String? = null,
    val displayName: String? = null,
    val avatarUrl: String? = null,
    val entryCount: Int = 0,
    val latestEntryAt: String? = null,
    val hasUnseen: Boolean = false,
    val streakDays: Int? = null,
    val isSelf: Boolean = false
) {
    val nameOrFallback: String get() = displayName?.takeIf { it.isNotBlank() }
        ?: username?.takeIf { it.isNotBlank() }
        ?: "Kullanıcı"
}

@Serializable
data class DiaryFeedResponse(
    val rings: List<DiaryRing> = emptyList()
)

@Serializable
data class DiaryEntry(
    val id: String = "",
    @SerialName("media_type") val mediaType: String = "text",
    @SerialName("media_url") val mediaUrl: String? = null,
    @SerialName("poster_url") val posterUrl: String? = null,
    val caption: String? = null,
    @SerialName("goal_id") val goalId: String? = null,
    @SerialName("goal_title") val goalTitle: String? = null,
    val visibility: String = "private",
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class DiaryListResponse(
    val owner: UserProfile? = null,
    val entries: List<DiaryEntry> = emptyList(),
    val isSelf: Boolean = false
)

@Serializable
data class CreateDiaryInput(
    val mediaType: String,
    val mediaUrl: String? = null,
    val posterUrl: String? = null,
    val caption: String? = null,
    val visibility: String = "private",
    val goalId: String? = null
)

@Serializable
data class CreateDiaryResponse(
    val entry: DiaryEntry? = null,
    val error: String? = null
)

@Serializable
data class MarkDiarySeenInput(
    val ownerId: String
)

@Serializable
data class DeleteDiaryInput(
    val entryId: String
)
