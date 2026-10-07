package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_items")
data class SavedItemEntity(
    @PrimaryKey
    val id: String,
    val tmdbId: String,
    val title: String,
    val poster: String?,
    val backdrop: String?,
    val type: String,
    val rating: Double?,
    val year: String?,
    val overview: String?,
    val savedAt: Long = System.currentTimeMillis()
)
