package io.lunosfer.dreamap.data.model

import kotlinx.serialization.Serializable

/** pages/api/explore/feed.js response şekli. */
@Serializable
data class ExploreFeedResponse(
    val dreams: List<Dream>,
    val page: Int,
    val hasMore: Boolean,
    val rankToken: String? = null
)
