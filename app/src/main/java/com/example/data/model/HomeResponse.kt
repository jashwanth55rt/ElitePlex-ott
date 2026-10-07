package com.example.data.model

import com.google.gson.annotations.SerializedName

data class HomeResponse(
    @SerializedName("creator")
    val creator: String? = null,
    @SerializedName("trending")
    val trending: List<MovieItem>? = null,
    @SerializedName("popular_movies")
    val popularMovies: List<MovieItem>? = null,
    @SerializedName("popular_tv")
    val popularTv: List<MovieItem>? = null,
    @SerializedName("top_movies")
    val topMovies: List<MovieItem>? = null,
    @SerializedName("top_tv")
    val topTv: List<MovieItem>? = null,
    @SerializedName("netplay_admin")
    val netplayAdmin: List<MovieItem>? = null
)
