package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.MovieItem
import com.example.data.repository.SearchRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface SearchUiState {
    object Idle : SearchUiState
    object Loading : SearchUiState
    data class Success(val query: String, val results: List<MovieItem>) : SearchUiState
    data class Empty(val query: String) : SearchUiState
    data class Error(val message: String) : SearchUiState
}

class SearchViewModel(
    private val searchRepository: SearchRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<SearchUiState>(SearchUiState.Idle)
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val _recentSearches = MutableStateFlow<List<String>>(
        listOf("Anime", "Naruto", "Attack on Titan", "Spider-Man", "Avatar", "Game of Thrones", "Breaking Bad")
    )
    val recentSearches: StateFlow<List<String>> = _recentSearches.asStateFlow()

    private var searchJob: Job? = null

    fun onQueryChanged(query: String) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            searchJob?.cancel()
            _uiState.value = SearchUiState.Idle
            return
        }

        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(400) // Debounce 400ms
            performSearch(trimmed)
        }
    }

    fun submitSearch(query: String) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return
        searchJob?.cancel()
        viewModelScope.launch {
            performSearch(trimmed)
        }
    }

    private suspend fun performSearch(query: String) {
        _uiState.value = SearchUiState.Loading

        // Update recent searches
        val currentRecent = _recentSearches.value.toMutableList()
        currentRecent.remove(query)
        currentRecent.add(0, query)
        _recentSearches.value = currentRecent.take(10)

        val result = searchRepository.search(query)
        result.onSuccess { response ->
            val items = response.items.orEmpty()
            if (items.isEmpty()) {
                _uiState.value = SearchUiState.Empty(query)
            } else {
                _uiState.value = SearchUiState.Success(query, items)
            }
        }.onFailure { error ->
            _uiState.value = SearchUiState.Error(
                error.localizedMessage ?: "Failed to perform search. Please try again."
            )
        }
    }

    fun clearRecentSearches() {
        _recentSearches.value = emptyList()
    }
}
