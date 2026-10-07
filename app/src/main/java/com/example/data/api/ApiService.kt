package com.example.data.api

import com.example.data.model.CatalogResponse
import com.example.data.model.HomeResponse
import com.example.data.model.MbDetailResponse
import com.example.data.model.MovieDetailResponse
import com.example.data.model.PlayResponse
import com.example.data.model.SearchResponse
import com.example.data.model.SeasonEpisodesResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {

    @GET("api/home")
    suspend fun getHomeCatalog(): HomeResponse

    @GET("api/movies")
    suspend fun getMovies(
        @Query("page") page: Int = 1
    ): CatalogResponse

    @GET("api/series")
    suspend fun getSeries(
        @Query("page") page: Int = 1
    ): CatalogResponse

    @GET("api/search")
    suspend fun search(
        @Query("q") query: String
    ): SearchResponse

    @GET("api/detail/movie/{id}")
    suspend fun getMovieDetail(
        @Path("id") id: String
    ): MovieDetailResponse

    @GET("api/detail/tv/{id}")
    suspend fun getTvDetail(
        @Path("id") id: String
    ): MovieDetailResponse

    @GET("mb/detail/{id}")
    suspend fun getMbDetail(
        @Path("id") id: String
    ): MbDetailResponse

    @GET("api/tv/{id}/season/{season}")
    suspend fun getSeasonEpisodes(
        @Path("id") id: String,
        @Path("season") season: Int
    ): SeasonEpisodesResponse

    @GET("api/play")
    suspend fun getPlaySources(
        @Query("tmdb_id") tmdbId: String,
        @Query("media") media: String,
        @Query("fast") fast: Int = 1,
        @Query("se") season: Int? = null,
        @Query("ep") episode: Int? = null
    ): PlayResponse

    @GET("play")
    suspend fun getUnifiedPlay(
        @Query("subject_id") subjectId: String,
        @Query("se") season: Int = 0,
        @Query("ep") episode: Int = 0
    ): PlayResponse
}
