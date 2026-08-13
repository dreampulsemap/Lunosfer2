package io.lunosfer.dreamap.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ReferralStatsResponse(
    val ok: Boolean? = true,
    val code: String? = null,
    @SerialName("referralCode") val referralCode: String? = null,
    @SerialName("totalReferrals") val totalReferrals: Int = 0,
    @SerialName("totalManaEarned") val totalManaEarned: Int = 0,
    @SerialName("claimableMana") val claimableMana: Int = 0,
    val history: List<ReferralHistoryItem> = emptyList(),
    val error: String? = null
) {
    val displayCode: String get() = code ?: referralCode ?: "—"
}

@Serializable
data class ReferralHistoryItem(
    val id: String? = null,
    @SerialName("referredUser") val referredUser: String? = null,
    @SerialName("manaEarned") val manaEarned: Int = 0,
    @SerialName("createdAt") val createdAt: String? = null
)

@Serializable
data class ClaimReferralRequest(
    @SerialName("referralCode") val referralCode: String? = null,
    val code: String? = null
)

@Serializable
data class ClaimReferralResponse(
    val ok: Boolean? = true,
    @SerialName("manaAwarded") val manaAwarded: Int = 0,
    val message: String? = null,
    val error: String? = null
)
