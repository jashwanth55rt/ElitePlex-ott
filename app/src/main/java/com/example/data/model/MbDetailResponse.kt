package com.example.data.model

import com.google.gson.annotations.SerializedName

data class MbDetailResponse(
    @SerializedName("creator")
    val creator: String? = null,
    @SerializedName("provider")
    val provider: String? = null,
    @SerializedName("data")
    val data: MbDetailData? = null
) {
    fun toMovieDetailResponse(): MovieDetailResponse {
        val d = data
        val yr = d?.releaseDate?.take(4)
        val parsedGenres = d?.genre?.split(",")
            ?.map { it.trim() }
            ?.filter { it.isNotBlank() }
        val isTv = d?.subjectType == 2

        return MovieDetailResponse(
            id = d?.subjectId,
            tmdbId = d?.subjectId,
            name = d?.title,
            title = d?.title,
            overview = d?.description,
            poster = d?.cover?.url,
            backdrop = d?.stills?.url ?: d?.cover?.url,
            year = yr,
            rating = 8.0,
            genres = parsedGenres,
            type = if (isTv) "tv" else "movie",
            runtime = null,
            seasons = emptyList()
        )
    }
}

data class MbDetailData(
    @SerializedName("subjectId")
    val subjectId: String? = null,
    @SerializedName("subjectType")
    val subjectType: Int? = null,
    @SerializedName("title")
    val title: String? = null,
    @SerializedName("description")
    val description: String? = null,
    @SerializedName("releaseDate")
    val releaseDate: String? = null,
    @SerializedName("duration")
    val duration: String? = null,
    @SerializedName("genre")
    val genre: String? = null,
    @SerializedName("cover")
    val cover: MbImage? = null,
    @SerializedName("stills")
    val stills: MbImage? = null,
    @SerializedName("resourceDetectors")
    val resourceDetectors: List<MbResourceDetector>? = null
)

data class MbResourceDetector(
    @SerializedName("type")
    val type: Int? = null,
    @SerializedName("downloadUrl")
    val downloadUrl: String? = null,
    @SerializedName("resolutionList")
    val resolutionList: List<MbResolutionItem>? = null
)

data class MbResolutionItem(
    @SerializedName("resolution")
    val resolution: Int? = null,
    @SerializedName("resourceLink")
    val resourceLink: String? = null,
    @SerializedName("size")
    val size: Long? = null,
    @SerializedName("se")
    val season: Int? = null,
    @SerializedName("ep")
    val episode: Int? = null
)

data class MbImage(
    @SerializedName("url")
    val url: String? = null
)
