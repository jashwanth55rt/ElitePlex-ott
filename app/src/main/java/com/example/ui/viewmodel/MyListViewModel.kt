package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.DownloadDao
import com.example.data.local.DownloadItemEntity
import com.example.data.local.SavedItemEntity
import com.example.data.repository.SavedRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MyListViewModel(
    private val savedRepository: SavedRepository,
    private val downloadDao: DownloadDao
) : ViewModel() {

    val savedItems: StateFlow<List<SavedItemEntity>> = savedRepository.getAllSavedItems()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val downloads: StateFlow<List<DownloadItemEntity>> = downloadDao.getAllDownloads()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun removeItem(id: String) {
        viewModelScope.launch {
            savedRepository.removeItem(id)
        }
    }
}
