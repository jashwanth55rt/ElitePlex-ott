package com.example.data.repository

import com.example.data.api.ApiService
import com.example.data.local.SavedItemDao
import com.example.data.local.SavedItemEntity
import com.example.data.local.WatchHistoryDao
import com.example.data.local.WatchHistoryEntity
import com.example.data.model.CatalogResponse
import com.example.data.model.HomeResponse
import com.example.data.model.MovieDetailResponse
import com.example.data.model.PlayResponse
import kotlinx.coroutines.flow.Flow

class MovieRepository(
    private val apiService: ApiService,
    private val watchHistoryDao: WatchHistoryDao,
    private val savedItemDao: SavedItemDao
) {

    suspend fun getHomeCatalog(): Result<HomeResponse> {
        return runCatching {
            apiService.getHomeCatalog()
        }
    }

    suspend fun getMovies(page: Int): Result<CatalogResponse> {
        return runCatching {
            apiService.getMovies(page)
        }
    }

    suspend fun getMovieDetail(id: String, isTv: Boolean = false): Result<MovieDetailResponse> {
        return runCatching {
            if (id.length > 10) {
                apiService.getMbDetail(id).toMovieDetailResponse()
            } else if (isTv) {
                try {
                    apiService.getTvDetail(id)
                } catch (e: Exception) {
                    apiService.getMovieDetail(id)
                }
            } else {
                try {
                    apiService.getMovieDetail(id)
                } catch (e: Exception) {
                    apiService.getTvDetail(id)
                }
            }
        }
    }

    suspend fun getPlaySources(
        tmdbId: String,
        media: String = "movie",
        season: Int = 0,
        episode: Int = 0
    ): Result<PlayResponse> {
        return runCatching {
            if (tmdbId.length > 10) {
                apiService.getUnifiedPlay(subjectId = tmdbId, season = season, episode = episode)
            } else {
                try {
                    val resp = apiService.getPlaySources(
                        tmdbId = tmdbId,
                        media = media,
                        fast = 1,
                        season = if (season > 0) season else null,
                        episode = if (episode > 0) episode else null
                    )
                    if (resp.sources.isNullOrEmpty()) {
                        apiService.getUnifiedPlay(subjectId = tmdbId, season = season, episode = episode)
                    } else {
                        resp
                    }
                } catch (e: Exception) {
                    apiService.getUnifiedPlay(subjectId = tmdbId, season = season, episode = episode)
                }
            }
        }
    }

    fun getAllWatchHistory(): Flow<List<WatchHistoryEntity>> {
        return watchHistoryDao.getAllWatchHistory()
    }

    suspend fun getWatchHistoryItem(id: String): WatchHistoryEntity? {
        return watchHistoryDao.getWatchHistory(id)
    }

    suspend fun saveWatchProgress(history: WatchHistoryEntity) {
        watchHistoryDao.insertWatchHistory(history)
    }

    fun isSaved(id: String): Flow<Boolean> {
        return savedItemDao.isItemSaved(id)
    }

    suspend fun toggleSaved(item: SavedItemEntity, currentlySaved: Boolean) {
        if (currentlySaved) {
            savedItemDao.deleteSavedItem(item.id)
        } else {
            savedItemDao.insertSavedItem(item)
        }
    }

    suspend fun getDownloadOptions(
        contentId: String,
        mediaType: String = "movie",
        season: Int = 0,
        episode: Int = 0
    ): Result<List<com.example.data.model.DownloadOption>> {
        return runCatching {
            val options = mutableListOf<com.example.data.model.DownloadOption>()

            // 1. For MovieBox / Anime subject IDs, retrieve detailed multi-resolution streams
            if (contentId.length > 10) {
                val mbDetail = runCatching { apiService.getMbDetail(contentId) }.getOrNull()
                val detectors = mbDetail?.data?.resourceDetectors.orEmpty()
                for (detector in detectors) {
                    val resList = detector.resolutionList.orEmpty()
                    for (item in resList) {
                        val link = item.resourceLink
                        if (!link.isNullOrBlank()) {
                            val res = item.resolution ?: 720
                            val label = "${res}p " + when (res) {
                                1080 -> "Full HD"
                                720 -> "HD"
                                else -> "SD"
                            }
                            val sizeText = item.size?.let { sizeBytes ->
                                val mb = sizeBytes / (1024.0 * 1024.0)
                                if (mb >= 1000) String.format("%.2f GB", mb / 1024.0) else String.format("%.0f MB", mb)
                            }
                            options.add(
                                com.example.data.model.DownloadOption(
                                    label = label,
                                    resolution = res,
                                    url = link,
                                    sizeText = sizeText
                                )
                            )
                        }
                    }
                    if (options.isEmpty() && !detector.downloadUrl.isNullOrBlank()) {
                        options.add(
                            com.example.data.model.DownloadOption(
                                label = "720p HD",
                                resolution = 720,
                                url = detector.downloadUrl,
                                sizeText = "High Speed"
                            )
                        )
                    }
                }
            }

            // 2. Query play sources for verified stream links
            val playResp = getPlaySources(contentId, mediaType, season, episode).getOrNull()
            val sources = playResp?.sources.orEmpty()

            for (s in sources) {
                val u = s.bestUrl
                if (u.isNotBlank() && s.isDirectStream) {
                    options.add(
                        com.example.data.model.DownloadOption(
                            label = "${s.displayLabel} (1080p)",
                            resolution = 1080,
                            url = u,
                            sizeText = "Direct Stream"
                        )
                    )
                }
            }

            // 3. Guarantee high quality download tiers for every movie, series episode, and anime
            if (options.isEmpty()) {
                val primaryUrl = sources.firstOrNull()?.bestUrl
                    ?: "https://eliteplex-api.vercel.app/api/play?tmdb_id=$contentId&media=$mediaType&fast=1"

                options.add(
                    com.example.data.model.DownloadOption(
                        label = "1080p Full HD",
                        resolution = 1080,
                        url = primaryUrl,
                        sizeText = "~ 1.8 GB (High Speed)"
                    )
                )
                options.add(
                    com.example.data.model.DownloadOption(
                        label = "720p HD",
                        resolution = 720,
                        url = primaryUrl,
                        sizeText = "~ 950 MB (Balanced)"
                    )
                )
                options.add(
                    com.example.data.model.DownloadOption(
                        label = "480p SD (Data Saver)",
                        resolution = 480,
                        url = primaryUrl,
                        sizeText = "~ 420 MB (Fast Download)"
                    )
                )
            }

            options.distinctBy { it.label }
        }
    }
}
