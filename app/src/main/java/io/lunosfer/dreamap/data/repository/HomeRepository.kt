package io.lunosfer.dreamap.data.repository

import io.lunosfer.dreamap.data.model.FeedItem
import io.lunosfer.dreamap.data.network.NetworkModule
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.async

/**
 * pages/api/home-feed.js'nin type=dreams ve type=visions çağrılarını PARALEL
 * yapıp created_at'e göre tek bir kronolojik listede birleştirir — web
 * tarafındaki handler'ın yaptığı [...dreams, ...visions].sort() mantığının
 * istemci tarafı karşılığı (bkz. HomeFeed.kt açıklaması: neden ayrı iki
 * tip-güvenli çağrı tercih edildi). İki çağrı birbirinden bağımsız olduğu
 * için coroutineScope + async ile paralel çalıştırılıyor, ardışık değil.
 */
class HomeRepository {
    private val api = NetworkModule.api

    suspend fun loadFirstPage(): Result<List<FeedItem>> = runCatching {
        coroutineScope {
            val dreamsDeferred = async { api.getHomeDreams(type = "dreams", dreamsBefore = null) }
            val visionsDeferred = async { api.getHomeVisions(type = "visions", visionsBefore = null) }

            val dreamsResponse = dreamsDeferred.await()
            val visionsResponse = visionsDeferred.await()

            val items = buildList {
                addAll(dreamsResponse.items.map { FeedItem.DreamItem(it) })
                addAll(visionsResponse.items.map { FeedItem.VisionItem(it) })
            }

            items.sortedByDescending { it.createdAt }
        }
    }
}
