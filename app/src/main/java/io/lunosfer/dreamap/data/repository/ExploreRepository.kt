package io.lunosfer.dreamap.data.repository

import io.lunosfer.dreamap.data.model.Dream
import io.lunosfer.dreamap.data.network.NetworkModule

class ExploreRepository {
    private val api = NetworkModule.api

    suspend fun loadFirstPage(): Result<List<Dream>> = runCatching {
        api.getExploreFeed(page = 0, rankToken = null, asOf = null).dreams
    }
}
