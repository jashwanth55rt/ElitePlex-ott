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
        val topTv: List<MovieItem>,
        val netplayExclusives: List<MovieItem> = emptyList()
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
                val trending = data.trending.orEmpty().distinctBy { it.displayId }
                val popularMovies = data.popularMovies.orEmpty().distinctBy { it.displayId }
                val popularTv = data.popularTv.orEmpty().distinctBy { it.displayId }
                val topMovies = data.topMovies.orEmpty().distinctBy { it.displayId }
                val topTv = data.topTv.orEmpty().distinctBy { it.displayId }
                val netplay = data.netplayAdmin.orEmpty().distinctBy { it.displayId }

                val hero = trending.take(6).ifEmpty {
                    popularMovies.take(6)
                }

                // Extract high-quality anime & animation from catalog
                val animeCatalog = (topTv + trending + popularTv + topMovies).filter { item ->
                    val t = item.displayTitle.lowercase()
                    val o = (item.overview ?: "").lowercase()
                    t.contains("frieren") || t.contains("avatar") || t.contains("one piece") ||
                    t.contains("arcane") || t.contains("takopi") || t.contains("anime") ||
                    t.contains("hero") || t.contains("dragon") || o.contains("anime") || o.contains("manga")
                }.distinctBy { it.displayId }

                _uiState.value = HomeUiState.Success(
                    heroItems = hero,
                    trending = trending,
                    popularMovies = popularMovies,
                    popularTv = popularTv,
                    trendingAnime = animeCatalog,
                    topMovies = topMovies,
                    topTv = topTv,
                    netplayExclusives = netplay
                )
            }.onFailure { error ->
                _uiState.value = HomeUiState.Error(
                    error.localizedMessage ?: "Unable to load ElitePlex home catalog."
                )
            }
        }
    }
}
