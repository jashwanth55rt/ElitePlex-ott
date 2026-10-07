package com.example.data.repository

import com.example.data.api.ApiService
import com.example.data.model.CatalogResponse
import com.example.data.model.MovieDetailResponse
import com.example.data.model.SeasonEpisodesResponse

class SeriesRepository(
    private val apiService: ApiService
) {

    suspend fun getSeries(page: Int): Result<CatalogResponse> {
        return runCatching {
            apiService.getSeries(page)
        }
    }

    suspend fun getTvDetail(id: String): Result<MovieDetailResponse> {
        return runCatching {
            if (id.length > 10) {
                apiService.getMbDetail(id).toMovieDetailResponse()
            } else {
                try {
                    apiService.getTvDetail(id)
                } catch (e: Exception) {
                    apiService.getMovieDetail(id)
                }
            }
        }
    }

    suspend fun getSeasonEpisodes(id: String, season: Int): Result<SeasonEpisodesResponse> {
        return runCatching {
            apiService.getSeasonEpisodes(id, season)
        }
    }
}
