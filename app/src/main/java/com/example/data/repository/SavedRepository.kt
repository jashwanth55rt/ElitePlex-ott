package com.example.data.repository

import com.example.data.local.SavedItemDao
import com.example.data.local.SavedItemEntity
import kotlinx.coroutines.flow.Flow

class SavedRepository(
    private val savedItemDao: SavedItemDao
) {

    fun getAllSavedItems(): Flow<List<SavedItemEntity>> {
        return savedItemDao.getAllSavedItems()
    }

    fun isSaved(id: String): Flow<Boolean> {
        return savedItemDao.isItemSaved(id)
    }

    suspend fun saveItem(item: SavedItemEntity) {
        savedItemDao.insertSavedItem(item)
    }

    suspend fun removeItem(id: String) {
        savedItemDao.deleteSavedItem(id)
    }

    suspend fun toggleItem(item: SavedItemEntity): Boolean {
        val currentlySaved = savedItemDao.isItemSavedSync(item.id)
        return if (currentlySaved) {
            savedItemDao.deleteSavedItem(item.id)
            false
        } else {
            savedItemDao.insertSavedItem(item)
            true
        }
    }
}
