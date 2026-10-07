package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.SavedItemEntity
import com.example.data.model.Episode
import com.example.data.model.MovieDetailResponse
import com.example.data.model.MovieItem
import com.example.data.repository.MovieRepository
import com.example.data.repository.SavedRepository
import com.example.data.repository.SeriesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

sealed interface DetailsUiState {
    object Loading : DetailsUiState
    data class Success(
        val detail: MovieDetailResponse,
        val episodes: List<Episode> = emptyList(),
        val selectedSeason: Int = 1,
        val isSaved: Boolean = false,
        val moreLikeThis: List<MovieItem> = emptyList()
    ) : DetailsUiState
    data class Error(val message: String) : DetailsUiState
}

class DetailsViewModel(
    private val movieRepository: MovieRepository,
    private val seriesRepository: SeriesRepository,
    private val savedRepository: SavedRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<DetailsUiState>(DetailsUiState.Loading)
    val uiState: StateFlow<DetailsUiState> = _uiState.asStateFlow()

    private var currentId: String = ""
    private var isTv: Boolean = false

    fun loadDetails(
        id: String,
        isTvSeries: Boolean,
        fallbackTitle: String? = null,
        fallbackPoster: String? = null
    ) {
        currentId = id
        isTv = isTvSeries
        _uiState.value = DetailsUiState.Loading

        viewModelScope.launch {
            var detailResult = if (isTvSeries) {
                seriesRepository.getTvDetail(id, fallbackTitle, fallbackPoster)
            } else {
                movieRepository.getMovieDetail(id, isTv = false, fallbackTitle, fallbackPoster)
            }

            if (detailResult.isFailure) {
                val altResult = if (isTvSeries) {
                    movieRepository.getMovieDetail(id, isTv = false, fallbackTitle, fallbackPoster)
                } else {
                    seriesRepository.getTvDetail(id, fallbackTitle, fallbackPoster)
                }
                if (altResult.isSuccess) {
                    detailResult = altResult
                }
            }

            detailResult.onSuccess { detail ->
                // Load more like this from home trending
                val trending = movieRepository.getHomeCatalog().getOrNull()?.trending.orEmpty()
                val moreLikeThis = trending.filter { it.displayId != id }.take(10)

                // If TV series, fetch season 1 episodes
                val initialEpisodes = if (detail.isTvSeries) {
                    val firstSeason = detail.seasons?.firstOrNull()?.seasonNumber ?: 1
                    seriesRepository.getSeasonEpisodes(id, firstSeason).getOrNull()?.episodes.orEmpty()
                } else {
                    emptyList()
                }

                _uiState.value = DetailsUiState.Success(
                    detail = detail,
                    episodes = initialEpisodes,
                    selectedSeason = 1,
                    isSaved = false,
                    moreLikeThis = moreLikeThis
                )

                // Observe isSaved state from Room
                observeSavedState(detail.displayId)

            }.onFailure { error ->
                _uiState.value = DetailsUiState.Error(
                    error.localizedMessage ?: "Failed to load content details."
                )
            }
        }
    }

    private fun observeSavedState(id: String) {
        viewModelScope.launch {
            savedRepository.isSaved(id).collectLatest { isSaved ->
                val current = _uiState.value as? DetailsUiState.Success ?: return@collectLatest
                _uiState.value = current.copy(isSaved = isSaved)
            }
        }
    }

    fun selectSeason(seasonNumber: Int) {
        val current = _uiState.value as? DetailsUiState.Success ?: return
        if (!current.detail.isTvSeries) return

        viewModelScope.launch {
            val episodes = seriesRepository.getSeasonEpisodes(currentId, seasonNumber)
                .getOrNull()?.episodes.orEmpty()
            _uiState.value = current.copy(
                selectedSeason = seasonNumber,
                episodes = episodes
            )
        }
    }

    fun toggleSaved() {
        val current = _uiState.value as? DetailsUiState.Success ?: return
        val detail = current.detail
        viewModelScope.launch {
            if (current.isSaved) {
                savedRepository.removeItem(detail.displayId)
            } else {
                val entity = SavedItemEntity(
                    id = detail.displayId,
                    tmdbId = detail.tmdbId ?: detail.id ?: "",
                    title = detail.displayTitle,
                    poster = detail.poster,
                    backdrop = detail.backdrop,
                    type = if (detail.isTvSeries) "tv" else "movie",
                    rating = detail.rating,
                    year = detail.year,
                    overview = detail.overview
                )
                savedRepository.saveItem(entity)
            }
        }
    }
}
