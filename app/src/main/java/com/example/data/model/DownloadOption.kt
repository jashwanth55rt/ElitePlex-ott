package com.example.data.model

data class DownloadOption(
    val label: String,
    val resolution: Int,
    val url: String,
    val sizeText: String? = null
)
