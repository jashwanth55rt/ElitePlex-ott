package com.example.data.model

import com.google.gson.annotations.SerializedName

data class CatalogResponse(
    @SerializedName("creator")
    val creator: String? = null,
    @SerializedName("items")
    val items: List<MovieItem>? = null,
    @SerializedName("page")
    val page: Int? = null,
    @SerializedName("total_pages")
    val totalPages: Int? = null,
    @SerializedName("total_results")
    val totalResults: Int? = null,
    @SerializedName("provider")
    val provider: String? = null
)
