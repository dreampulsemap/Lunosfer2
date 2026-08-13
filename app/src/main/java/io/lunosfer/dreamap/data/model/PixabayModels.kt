package io.lunosfer.dreamap.data.model

import kotlinx.serialization.Serializable

@Serializable
data class PixabaySearchResponse(
    val total: Int = 0,
    val totalHits: Int = 0,
    val hits: List<PixabayHit> = emptyList()
)

@Serializable
data class PixabayHit(
    val id: Long,
    val tags: List<String> = emptyList(),
    val webformatURL: String,
    val previewURL: String? = null,
    val user: String,
    val width: Int = 0,
    val height: Int = 0
)

@Serializable
data class PixabayImageRequest(
    val pixabayId: Long,
    val imageUrl: String,
    val tags: String,
    val pixabayUser: String,
    val width: Int,
    val height: Int
)

@Serializable
data class PixabayImageResponse(
    val url: String? = null,
    val ok: Boolean? = true,
    val error: String? = null
)

@Serializable
data class PixabayVideoImportRequest(
    val pixabayId: Long,
    val videoUrl: String,
    val tags: String = "",
    val user: String = ""
)

@Serializable
data class PixabayVideoImportResponse(
    val url: String? = null,
    val ok: Boolean? = true,
    val error: String? = null
)

@Serializable
data class PixabayVideoSearchResponse(
    val total: Int = 0,
    val totalHits: Int = 0,
    val hits: List<PixabayVideoHit> = emptyList()
)

@Serializable
data class PixabayVideoHit(
    val id: Long,
    val tags: List<String> = emptyList(),
    val pageURL: String? = null,
    val duration: Int = 0,
    val user: String = "",
    val videos: PixabayVideoDetailsMap? = null
)

@Serializable
data class PixabayVideoDetailsMap(
    val large: PixabayVideoFormat? = null,
    val medium: PixabayVideoFormat? = null,
    val small: PixabayVideoFormat? = null,
    val tiny: PixabayVideoFormat? = null
)

@Serializable
data class PixabayVideoFormat(
    val url: String = "",
    val width: Int = 0,
    val height: Int = 0,
    val size: Long = 0,
    val thumbnail: String? = null
)

