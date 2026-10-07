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
    private var isFetching = false
    private val allMovies = mutableListOf<MovieItem>()

    init {
        loadMovies(page = 1)
    }

    fun refresh() {
        currentPage = 1
        allMovies.clear()
        loadMovies(page = 1)
    }

    fun loadNextPage() {
        if (isFetching || currentPage >= totalPages) return
        val currentSuccess = _uiState.value as? MoviesUiState.Success ?: return
        _uiState.value = currentSuccess.copy(isLoadingMore = true)
        loadMovies(page = currentPage + 1)
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

                val newItems = catalog.items.orEmpty()
                if (page == 1) {
                    allMovies.clear()
                }
                allMovies.addAll(newItems)

                _uiState.value = MoviesUiState.Success(
                    items = allMovies.toList(),
                    page = currentPage,
                    totalPages = totalPages,
                    isLoadingMore = false
                )
            }.onFailure { error ->
                if (allMovies.isEmpty()) {
                    _uiState.value = MoviesUiState.Error(
                        error.localizedMessage ?: "Unable to load movies."
                    )
                } else {
                    val current = _uiState.value as? MoviesUiState.Success
                    if (current != null) {
                        _uiState.value = current.copy(isLoadingMore = false)
                    }
                }
            }
            isFetching = false
        }
    }
}
