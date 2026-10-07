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
    private var isFetching = false
    private val allSeries = mutableListOf<MovieItem>()

    init {
        loadSeries(page = 1)
    }

    fun refresh() {
        currentPage = 1
        allSeries.clear()
        loadSeries(page = 1)
    }

    fun loadNextPage() {
        if (isFetching || currentPage >= totalPages) return
        val currentSuccess = _uiState.value as? SeriesUiState.Success ?: return
        _uiState.value = currentSuccess.copy(isLoadingMore = true)
        loadSeries(page = currentPage + 1)
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

                val newItems = catalog.items.orEmpty()
                if (page == 1) {
                    allSeries.clear()
                }
                allSeries.addAll(newItems)

                _uiState.value = SeriesUiState.Success(
                    items = allSeries.toList(),
                    page = currentPage,
                    totalPages = totalPages,
                    isLoadingMore = false
                )
            }.onFailure { error ->
                if (allSeries.isEmpty()) {
                    _uiState.value = SeriesUiState.Error(
                        error.localizedMessage ?: "Unable to load TV series."
                    )
                } else {
                    val current = _uiState.value as? SeriesUiState.Success
                    if (current != null) {
                        _uiState.value = current.copy(isLoadingMore = false)
                    }
                }
            }
            isFetching = false
        }
    }
}
