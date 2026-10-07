package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.MovieItem
import com.example.data.repository.SeriesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface SeriesUiState {
    object Loading : SeriesUiState
    data class Success(
        val items: List<MovieItem>,
        val page: Int,
        val totalPages: Int,
        val totalResults: Int = 0,
        val selectedGenre: String = "All",
        val isLoadingMore: Boolean = false
    ) : SeriesUiState
    data class Error(val message: String) : SeriesUiState
}

class SeriesViewModel(
    private val seriesRepository: SeriesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<SeriesUiState>(SeriesUiState.Loading)
    val uiState: StateFlow<SeriesUiState> = _uiState.asStateFlow()

    private var currentPage = 1
    private var totalPages = 1
    private var totalResults = 0
    private var isFetching = false
    private val allSeries = mutableListOf<MovieItem>()
    private var currentGenre = "All"

    init {
        loadSeries(page = 1)
    }

    fun refresh() {
        currentPage = 1
        allSeries.clear()
        loadSeries(page = 1)
    }

    fun filterByGenre(genre: String) {
        currentGenre = genre
        emitCurrentState(isLoadingMore = false)
    }

    fun loadNextPage() {
        if (isFetching || currentPage >= totalPages) return
        val currentSuccess = _uiState.value as? SeriesUiState.Success ?: return
        _uiState.value = currentSuccess.copy(isLoadingMore = true)
        loadSeries(page = currentPage + 1)
    }

    private fun emitCurrentState(isLoadingMore: Boolean) {
        val filtered = if (currentGenre == "All") {
            allSeries.toList()
        } else {
            val query = currentGenre.lowercase()
            allSeries.filter { item ->
                val title = item.displayTitle.lowercase()
                val overview = (item.overview ?: "").lowercase()
                title.contains(query) || overview.contains(query) ||
                (query == "anime" && (title.contains("animation") || overview.contains("anime") || overview.contains("manga")))
            }
        }

        _uiState.value = SeriesUiState.Success(
            items = filtered,
            page = currentPage,
            totalPages = totalPages,
            totalResults = if (totalResults > 0) totalResults else allSeries.size,
            selectedGenre = currentGenre,
            isLoadingMore = isLoadingMore
        )
    }

    private fun loadSeries(page: Int) {
        if (isFetching) return
        isFetching = true

        viewModelScope.launch {
            if (page == 1 && allSeries.isEmpty()) {
                _uiState.value = SeriesUiState.Loading
            }

            val result = seriesRepository.getSeries(page)
            result.onSuccess { catalog ->
                currentPage = catalog.page ?: page
                totalPages = catalog.totalPages ?: 1
                totalResults = catalog.totalResults ?: totalResults

                val newItems = catalog.items.orEmpty()
                if (page == 1) {
                    allSeries.clear()
                }

                // Deduplicate by displayId to avoid DiffUtil crashes
                val existingIds = allSeries.map { it.displayId }.toSet()
                val uniqueNew = newItems.filter { it.displayId !in existingIds }
                allSeries.addAll(uniqueNew)

                emitCurrentState(isLoadingMore = false)
            }.onFailure { error ->
                if (allSeries.isEmpty()) {
                    _uiState.value = SeriesUiState.Error(
                        error.localizedMessage ?: "Unable to load TV series."
                    )
                } else {
                    emitCurrentState(isLoadingMore = false)
                }
            }
            isFetching = false
        }
    }
}
