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

    suspend fun getTvDetail(
        id: String,
        fallbackTitle: String? = null,
        fallbackPoster: String? = null
    ): Result<MovieDetailResponse> {
        return runCatching {
            if (id.contains("-") && id.length >= 30) {
                MovieDetailResponse(
                    id = id,
                    tmdbId = id,
                    name = fallbackTitle ?: "Exclusive Series",
                    title = fallbackTitle ?: "Exclusive Series",
                    overview = "NetPlay Exclusive Series streaming in high quality.",
                    poster = fallbackPoster,
                    backdrop = fallbackPoster,
                    year = "2026",
                    rating = 8.5,
                    genres = listOf("Series", "NetPlay"),
                    type = "tv",
                    runtime = null,
                    seasons = listOf(
                        com.example.data.model.SeasonInfo(seasonNumber = 1, name = "Season 1", episodeCount = 10)
                    )
                )
            } else if (id.length > 10) {
                try {
                    apiService.getMbDetail(id).toMovieDetailResponse()
                } catch (e: Exception) {
                    MovieDetailResponse(
                        id = id,
                        tmdbId = id,
                        name = fallbackTitle ?: "Series",
                        title = fallbackTitle ?: "Series",
                        overview = "Stream episodes with high speed cloud streaming on ElitePlex.",
                        poster = fallbackPoster,
                        backdrop = fallbackPoster,
                        year = "2026",
                        rating = 8.3,
                        genres = listOf("Series"),
                        type = "tv",
                        runtime = null,
                        seasons = listOf(
                            com.example.data.model.SeasonInfo(seasonNumber = 1, name = "Season 1", episodeCount = 10)
                        )
                    )
                }
            } else {
                try {
                    apiService.getTvDetail(id)
                } catch (e: Exception) {
                    try {
                        apiService.getMovieDetail(id)
                    } catch (e2: Exception) {
                        MovieDetailResponse(
                            id = id,
                            tmdbId = id,
                            name = fallbackTitle ?: "TV Series",
                            title = fallbackTitle ?: "TV Series",
                            overview = "Popular series available on ElitePlex.",
                            poster = fallbackPoster,
                            backdrop = fallbackPoster,
                            year = "2026",
                            rating = 8.4,
                            genres = listOf("Drama", "Series"),
                            type = "tv",
                            runtime = null,
                            seasons = listOf(
                                com.example.data.model.SeasonInfo(seasonNumber = 1, name = "Season 1", episodeCount = 10)
                            )
                        )
                    }
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
