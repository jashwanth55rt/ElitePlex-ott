package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchHistoryDao {

    @Query("SELECT * FROM watch_history ORDER BY lastWatchedAt DESC")
    fun getAllWatchHistory(): Flow<List<WatchHistoryEntity>>

    @Query("SELECT * FROM watch_history WHERE id = :id LIMIT 1")
    suspend fun getWatchHistory(id: String): WatchHistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWatchHistory(history: WatchHistoryEntity)

    @Query("DELETE FROM watch_history WHERE id = :id")
    suspend fun deleteWatchHistory(id: String)

    @Query("DELETE FROM watch_history")
    suspend fun clearWatchHistory()
}
