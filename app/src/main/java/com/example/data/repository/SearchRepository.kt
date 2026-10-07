package com.example.data.repository

import com.example.data.api.ApiService
import com.example.data.model.SearchResponse

class SearchRepository(
    private val apiService: ApiService
) {

    suspend fun search(query: String): Result<SearchResponse> {
        return runCatching {
            apiService.search(query)
        }
    }
}
