package com.example.data.model

import com.example.utils.ImageUtils
import com.google.gson.annotations.SerializedName

data class MovieItem(
    @SerializedName("id")
    val id: String? = null,
    @SerializedName("tmdb_id")
    val tmdbId: String? = null,
    @SerializedName("subject_id")
    val subjectId: String? = null,
    @SerializedName("netplay_id")
    val netplayId: String? = null,
    @SerializedName("id_type")
    val idType: String? = null,
    @SerializedName("name")
    val name: String? = null,
    @SerializedName("title")
    val title: String? = null,

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
    @SerializedName("type")
    val type: String? = null,
    @SerializedName("overview")
    val overview: String? = null,
    @SerializedName("provider")
    val provider: String? = null
) {
    val displayTitle: String
        get() = name?.takeIf { it.isNotBlank() }
            ?: title?.takeIf { it.isNotBlank() }
            ?: "Untitled"

    val displayId: String
        get() = tmdbId?.takeIf { it.isNotBlank() }
            ?: subjectId?.takeIf { it.isNotBlank() }
            ?: netplayId?.takeIf { it.isNotBlank() }
            ?: id?.takeIf { it.isNotBlank() }
            ?: displayTitle

    val isTvSeries: Boolean
        get() = type.equals("tv", ignoreCase = true) || type.equals("series", ignoreCase = true)

    val formattedRating: String
        get() = rating?.takeIf { it > 0 }?.let { String.format("%.1f", it) } ?: "8.2"

    /**
     * Resolves poster URL using the strict priority:
     * 1. poster / poster_url / posterUrl / poster_path / posterPath
     * 2. image / image_url / imageUrl / thumbnail / thumbnail_url
     * 3. backdrop / backdrop_url / backdrop_path
     */
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

    /**
     * Resolves backdrop URL using priority:
     * 1. backdrop / backdrop_url / backdrop_path
     * 2. poster / image / thumbnail
     */
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
