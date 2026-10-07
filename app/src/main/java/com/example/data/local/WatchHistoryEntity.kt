package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "watch_history")
data class WatchHistoryEntity(
    @PrimaryKey
    val id: String,
    val tmdbId: String,
    val title: String,
    val poster: String?,
    val backdrop: String?,
    val type: String,
    val seasonNumber: Int = 0,
    val episodeNumber: Int = 0,
    val playbackPosition: Long = 0L,
    val duration: Long = 0L,
    val lastWatchedAt: Long = System.currentTimeMillis()
) {
    val progressPercentage: Int
        get() = if (duration > 0) ((playbackPosition * 100) / duration).toInt().coerceIn(0, 100) else 0
}
