package com.example.data.model

import com.example.utils.ImageUtils
import com.google.gson.annotations.SerializedName

data class MovieDetailResponse(
    @SerializedName("id")
    val id: String? = null,
    @SerializedName("tmdb_id")
    val tmdbId: String? = null,
    @SerializedName("imdb_id")
    val imdbId: String? = null,
    @SerializedName("name")
    val name: String? = null,
    @SerializedName("title")
    val title: String? = null,
    @SerializedName("overview")
    val overview: String? = null,

    // Primary poster fields
    @SerializedName("poster")
    val poster: String? = null,
    @SerializedName("poster_url")
    val posterUrlSnake: String? = null,
    @SerializedName("posterUrl")
    val posterUrlCamel: String? = null,
    @SerializedName("poster_path")
    val posterPathSnake: String? = null,
    @SerializedName("posterPath")
    val posterPathCamel: String? = null,

    // Image / thumbnail fallbacks
    @SerializedName("image")
    val image: String? = null,
    @SerializedName("image_url")
    val imageUrlSnake: String? = null,
    @SerializedName("imageUrl")
    val imageUrlCamel: String? = null,
    @SerializedName("thumbnail")
    val thumbnail: String? = null,
    @SerializedName("thumbnail_url")
    val thumbnailUrlSnake: String? = null,

    // Backdrop fallbacks
    @SerializedName("backdrop")
    val backdrop: String? = null,
    @SerializedName("backdrop_url")
    val backdropUrlSnake: String? = null,
    @SerializedName("backdrop_path")
    val backdropPathSnake: String? = null,

    @SerializedName("year")
    val year: String? = null,
    @SerializedName("rating")
    val rating: Double? = null,
    @SerializedName("genres")
    val genres: List<String>? = null,
    @SerializedName("type")
    val type: String? = null,
    @SerializedName("runtime")
    val runtime: Int? = null,
    @SerializedName("seasons")
    val seasons: List<SeasonInfo>? = null
) {
    val displayTitle: String
        get() = name?.takeIf { it.isNotBlank() }
            ?: title?.takeIf { it.isNotBlank() }
            ?: "Untitled"

    val displayId: String
        get() = tmdbId?.takeIf { it.isNotBlank() }
            ?: id
            ?: ""

    val isTvSeries: Boolean
        get() = type.equals("tv", ignoreCase = true) || type.equals("series", ignoreCase = true) || !seasons.isNullOrEmpty()

    val formattedRating: String
        get() = rating?.let { String.format("%.1f", it) } ?: "8.2"

    val formattedRuntime: String
        get() = runtime?.let { "$it min" } ?: ""

    val genresFormatted: String
        get() = genres?.joinToString(" • ") ?: ""

    val resolvedPoster: String?
        get() = ImageUtils.getBestImageUrl(
            poster,
            posterUrlSnake,
            posterUrlCamel,
            posterPathSnake,
            posterPathCamel,
            image,
            imageUrlSnake,
            imageUrlCamel,
            thumbnail,
            thumbnailUrlSnake,
            backdrop,
            backdropUrlSnake,
            backdropPathSnake
        )

    val resolvedBackdrop: String?
        get() = ImageUtils.getBestImageUrl(
            backdrop,
            backdropUrlSnake,
            backdropPathSnake,
            poster,
            posterUrlSnake,
            posterUrlCamel,
            image,
            imageUrlSnake,
            thumbnail
        )
}

data class SeasonInfo(
    @SerializedName("season")
    val seasonNumber: Int? = null,
    @SerializedName("name")
    val name: String? = null,
    @SerializedName("episode_count")
    val episodeCount: Int? = null,
    @SerializedName("poster")
    val poster: String? = null
) {
    val displayTitle: String
        get() = name?.takeIf { it.isNotBlank() } ?: "Season ${seasonNumber ?: 1}"
}
