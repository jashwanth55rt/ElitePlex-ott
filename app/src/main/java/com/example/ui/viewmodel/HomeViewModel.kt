package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.WatchHistoryEntity
import com.example.data.model.MovieItem
import com.example.data.repository.MovieRepository
import com.example.data.repository.SearchRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface HomeUiState {
    object Loading : HomeUiState
    data class Success(
        val heroItems: List<MovieItem>,
        val trending: List<MovieItem>,
        val popularMovies: List<MovieItem>,
        val popularTv: List<MovieItem>,
        val trendingAnime: List<MovieItem>,
        val topMovies: List<MovieItem>,
        val topTv: List<MovieItem>
    ) : HomeUiState
    data class Error(val message: String) : HomeUiState
}

class HomeViewModel(
    private val movieRepository: MovieRepository,
    private val searchRepository: SearchRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    val watchHistory: StateFlow<List<WatchHistoryEntity>> = movieRepository.getAllWatchHistory()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        loadHomeCatalog()
    }

    fun loadHomeCatalog() {
        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading
            val result = movieRepository.getHomeCatalog()
            result.onSuccess { data ->
                val trending = data.trending.orEmpty()
                val popularMovies = data.popularMovies.orEmpty()
                val popularTv = data.popularTv.orEmpty()
                val topMovies = data.topMovies.orEmpty()
                val topTv = data.topTv.orEmpty()

                val hero = trending.take(5).ifEmpty {
                    popularMovies.take(5)
                }

                val anime = searchRepository.search("anime").getOrNull()?.items.orEmpty()

                _uiState.value = HomeUiState.Success(
                    heroItems = hero,
                    trending = trending,
                    popularMovies = popularMovies,
                    popularTv = popularTv,
                    trendingAnime = anime,
                    topMovies = topMovies,
                    topTv = topTv
                )
            }.onFailure { error ->
                _uiState.value = HomeUiState.Error(
                    error.localizedMessage ?: "Unable to load ElitePlex home catalog."
                )
            }
        }
    }
}
