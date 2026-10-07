package com.example.data.model

import com.google.gson.annotations.SerializedName

data class SearchResponse(
    @SerializedName("creator")
    val creator: String? = null,
    @SerializedName("query")
    val query: String? = null,
    @SerializedName("count")
    val count: Int? = null,
    @SerializedName("items")
    val items: List<MovieItem>? = null
)
