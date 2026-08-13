package io.lunosfer.dreamap.data.repository

import io.lunosfer.dreamap.data.model.ClaimReferralRequest
import io.lunosfer.dreamap.data.model.ClaimReferralResponse
import io.lunosfer.dreamap.data.model.ReferralStatsResponse
import io.lunosfer.dreamap.data.network.LunosferApi
import io.lunosfer.dreamap.data.network.NetworkModule

class ReferralRepository(
    private val api: LunosferApi = NetworkModule.api
) {
    suspend fun getReferralStats(): Result<ReferralStatsResponse> = runCatching {
        val res = api.getReferralStats()
        if (res.ok == false && res.error != null) {
            throw Exception(res.error)
        }
        res
    }

    suspend fun claimReferral(code: String): Result<ClaimReferralResponse> = runCatching {
        val res = api.claimReferral(ClaimReferralRequest(referralCode = code, code = code))
        if (res.ok == false && res.error != null) {
            throw Exception(res.error)
        }
        res
    }
}
