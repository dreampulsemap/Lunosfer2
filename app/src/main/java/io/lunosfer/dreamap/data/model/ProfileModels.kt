package io.lunosfer.dreamap.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UpdateProfileRequest(
    val userId: String,
    val username: String? = null,
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("is_private") val isPrivate: Boolean? = null,
    val language: String? = null,
    val gender: String? = null
)

@Serializable
data class FullUserProfile(
    val id: String = "",
    val username: String? = null,
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("is_private") val isPrivate: Boolean = false,
    val language: String? = null,
    val gender: String? = null,
    val bio: String? = null
) {
    val nameOrFallback: String get() = displayName?.takeIf { it.isNotBlank() }
        ?: username?.takeIf { it.isNotBlank() }
        ?: "?"
}

@Serializable
data class UpdateProfileResponse(
    val success: Boolean = false,
    val profile: FullUserProfile? = null,
    val error: String? = null,
    val message: String? = null
)

@Serializable
data class PremiumStatusResponse(
    val isPremium: Boolean = false,
    val canPickVideo: Boolean = true,
    val nextAvailableAt: String? = null
)
