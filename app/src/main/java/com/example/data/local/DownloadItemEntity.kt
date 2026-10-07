package com.example.data.local

import android.app.DownloadManager
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "downloads")
data class DownloadItemEntity(
    @PrimaryKey
    val id: String,
    val downloadManagerId: Long,
    val contentId: String,
    val title: String,
    val subtitle: String?,
    val poster: String?,
    val mediaType: String,
    val seasonNumber: Int = 0,
    val episodeNumber: Int = 0,
    val downloadUrl: String,
    val localUri: String? = null,
    val status: Int = DownloadManager.STATUS_RUNNING,
    val bytesDownloaded: Long = 0L,
    val totalBytes: Long = 0L,
    val quality: String = "1080p",
    val createdAt: Long = System.currentTimeMillis()
) {
    val isCompleted: Boolean
        get() = status == DownloadManager.STATUS_SUCCESSFUL

    val isDownloading: Boolean
        get() = status == DownloadManager.STATUS_RUNNING || status == DownloadManager.STATUS_PENDING

    val progressPercent: Int
        get() = if (totalBytes > 0) ((bytesDownloaded * 100) / totalBytes).toInt().coerceIn(0, 100) else 0

    val formattedSize: String
        get() {
            if (totalBytes <= 0) return "Calculating..."
            val mb = totalBytes / (1024.0 * 1024.0)
            return if (mb >= 1000) {
                String.format("%.2f GB", mb / 1024.0)
            } else {
                String.format("%.1f MB", mb)
            }
        }
}
