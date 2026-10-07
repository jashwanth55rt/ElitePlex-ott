package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedItemDao {

    @Query("SELECT * FROM saved_items ORDER BY savedAt DESC")
    fun getAllSavedItems(): Flow<List<SavedItemEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM saved_items WHERE id = :id)")
    fun isItemSaved(id: String): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM saved_items WHERE id = :id)")
    suspend fun isItemSavedSync(id: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavedItem(item: SavedItemEntity)

    @Query("DELETE FROM saved_items WHERE id = :id")
    suspend fun deleteSavedItem(id: String)
}
