package io.lunosfer.dreamap.data.repository

import io.lunosfer.dreamap.data.model.GenerateSummaryRequest
import io.lunosfer.dreamap.data.model.SummaryData
import io.lunosfer.dreamap.data.network.LunosferApi
import io.lunosfer.dreamap.data.network.NetworkModule

class SummaryRepository(
    private val api: LunosferApi = NetworkModule.api
) {
    suspend fun getLatestSummary(periodType: String): Result<SummaryData?> = runCatching {
        val res = api.getLatestSummary(periodType)
        if (res.ok == false && res.error != null) {
            throw Exception(res.error)
        }
        res.summary
    }

    suspend fun generateSummary(periodType: String): Result<SummaryData?> = runCatching {
        val res = api.generateSummary(GenerateSummaryRequest(periodType))
        if (res.ok == false && res.error != null) {
            throw Exception(res.error)
        }
        res.summary
    }
}
