package com.example.data.model

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
    @SerializedName("poster")
    val poster: String? = null,
    @SerializedName("backdrop")
    val backdrop: String? = null,
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
        get() = rating?.let { String.format("%.1f", it) } ?: "N/A"

    val formattedRuntime: String
        get() = runtime?.let { "$it min" } ?: ""

    val genresFormatted: String
        get() = genres?.joinToString(" • ") ?: ""
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
