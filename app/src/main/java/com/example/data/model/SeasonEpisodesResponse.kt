package com.example.data.model

import com.google.gson.annotations.SerializedName

data class SeasonEpisodesResponse(
    @SerializedName("creator")
    val creator: String? = null,
    @SerializedName("season")
    val season: Int? = null,
    @SerializedName("episodes")
    val episodes: List<Episode>? = null
)

data class Episode(
    @SerializedName("episode")
    val episodeNumber: Int? = null,
    @SerializedName("name")
    val name: String? = null,
    @SerializedName("overview")
    val overview: String? = null,
    @SerializedName("still")
    val still: String? = null
) {
    val displayTitle: String
        get() = name?.takeIf { it.isNotBlank() } ?: "Episode ${episodeNumber ?: 1}"
}
