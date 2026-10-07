package com.example.data.model

import com.google.gson.annotations.SerializedName

data class MovieItem(
    @SerializedName("id")
    val id: String? = null,
    @SerializedName("tmdb_id")
    val tmdbId: String? = null,
    @SerializedName("subject_id")
    val subjectId: String? = null,
    @SerializedName("id_type")
    val idType: String? = null,
    @SerializedName("name")
    val name: String? = null,
    @SerializedName("title")
    val title: String? = null,
    @SerializedName("poster")
    val poster: String? = null,
    @SerializedName("backdrop")
    val backdrop: String? = null,
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
            ?: id?.takeIf { it.isNotBlank() }
            ?: displayTitle

    val isTvSeries: Boolean
        get() = type.equals("tv", ignoreCase = true) || type.equals("series", ignoreCase = true)

    val formattedRating: String
        get() = rating?.takeIf { it > 0 }?.let { String.format("%.1f", it) } ?: "8.2"
}
