package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.MovieItem
import com.example.data.repository.MovieRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface MoviesUiState {
    object Loading : MoviesUiState
    data class Success(
        val items: List<MovieItem>,
        val page: Int,
        val totalPages: Int,
        val totalResults: Int = 0,
        val selectedGenre: String = "All",
        val isLoadingMore: Boolean = false
    ) : MoviesUiState
    data class Error(val message: String) : MoviesUiState
}

class MoviesViewModel(
    private val movieRepository: MovieRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<MoviesUiState>(MoviesUiState.Loading)
    val uiState: StateFlow<MoviesUiState> = _uiState.asStateFlow()

    private var currentPage = 1
    private var totalPages = 1
    private var totalResults = 0
    private var isFetching = false
    private val allMovies = mutableListOf<MovieItem>()
    private var currentGenre = "All"

    init {
        loadMovies(page = 1)
    }

    fun refresh() {
        currentPage = 1
        allMovies.clear()
        loadMovies(page = 1)
    }

    fun filterByGenre(genre: String) {
        currentGenre = genre
        emitCurrentState(isLoadingMore = false)
    }

    fun loadNextPage() {
        if (isFetching || currentPage >= totalPages) return
        val currentSuccess = _uiState.value as? MoviesUiState.Success ?: return
        _uiState.value = currentSuccess.copy(isLoadingMore = true)
        loadMovies(page = currentPage + 1)
    }

    private fun emitCurrentState(isLoadingMore: Boolean) {
        val filtered = if (currentGenre == "All") {
            allMovies.toList()
        } else {
            val query = currentGenre.lowercase()
            allMovies.filter { item ->
                val title = item.displayTitle.lowercase()
                val overview = (item.overview ?: "").lowercase()
                title.contains(query) || overview.contains(query) ||
                (query == "anime" && (title.contains("animation") || overview.contains("anime") || overview.contains("manga")))
            }
        }

        _uiState.value = MoviesUiState.Success(
            items = filtered,
            page = currentPage,
            totalPages = totalPages,
            totalResults = if (totalResults > 0) totalResults else allMovies.size,
            selectedGenre = currentGenre,
            isLoadingMore = isLoadingMore
        )
    }

    private fun loadMovies(page: Int) {
        if (isFetching) return
        isFetching = true

        viewModelScope.launch {
            if (page == 1 && allMovies.isEmpty()) {
                _uiState.value = MoviesUiState.Loading
            }

            val result = movieRepository.getMovies(page)
            result.onSuccess { catalog ->
                currentPage = catalog.page ?: page
                totalPages = catalog.totalPages ?: 1
                totalResults = catalog.totalResults ?: totalResults

                val newItems = catalog.items.orEmpty()
                if (page == 1) {
                    allMovies.clear()
                }

                // Deduplicate by displayId to prevent DiffUtil duplicate crashes
                val existingIds = allMovies.map { it.displayId }.toSet()
                val uniqueNew = newItems.filter { it.displayId !in existingIds }
                allMovies.addAll(uniqueNew)

                emitCurrentState(isLoadingMore = false)
            }.onFailure { error ->
                if (allMovies.isEmpty()) {
                    _uiState.value = MoviesUiState.Error(
                        error.localizedMessage ?: "Unable to load movies."
                    )
                } else {
                    emitCurrentState(isLoadingMore = false)
                }
            }
            isFetching = false
        }
    }
}
