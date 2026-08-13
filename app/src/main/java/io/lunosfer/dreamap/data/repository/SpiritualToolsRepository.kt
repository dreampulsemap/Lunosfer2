package io.lunosfer.dreamap.data.repository

import io.lunosfer.dreamap.data.model.*
import io.lunosfer.dreamap.data.network.LunosferApi
import io.lunosfer.dreamap.data.network.NetworkModule

class SpiritualToolsRepository(
    private val api: LunosferApi = NetworkModule.api
) {
    suspend fun generateMentalWall(lang: String = "tr"): Result<MentalWallResponse> = runCatching {
        val res = api.generateMentalWall(MentalWallRequest(lang))
        if (res.ok == false && res.error != null) {
            throw Exception(res.error)
        }
        res
    }

    suspend fun getPsycheMap(): Result<PsycheMapResponse> = runCatching {
        val res = api.getPsycheMap()
        if (res.ok == false && res.error != null) {
            throw Exception(res.error)
        }
        res
    }

    suspend fun consultProphet(question: String? = null, lang: String = "tr"): Result<ProphetResponse> = runCatching {
        val res = api.consultProphet(ProphetRequest(question, lang))
        if (res.ok == false && res.error != null) {
            throw Exception(res.error)
        }
        res
    }
}
