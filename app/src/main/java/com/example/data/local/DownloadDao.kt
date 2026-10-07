package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {

    @Query("SELECT * FROM downloads ORDER BY createdAt DESC")
    fun getAllDownloads(): Flow<List<DownloadItemEntity>>

    @Query("SELECT * FROM downloads ORDER BY createdAt DESC")
    suspend fun getAllDownloadsList(): List<DownloadItemEntity>

    @Query("SELECT * FROM downloads WHERE id = :id LIMIT 1")
    suspend fun getDownload(id: String): DownloadItemEntity?

    @Query("SELECT * FROM downloads WHERE contentId = :contentId")
    suspend fun getDownloadsForContent(contentId: String): List<DownloadItemEntity>

    @Query("SELECT * FROM downloads WHERE downloadManagerId = :dmId LIMIT 1")
    suspend fun getDownloadByDmId(dmId: Long): DownloadItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDownload(download: DownloadItemEntity)

    @Update
    suspend fun updateDownload(download: DownloadItemEntity)

    @Query("UPDATE downloads SET status = :status, localUri = :localUri WHERE downloadManagerId = :dmId")
    suspend fun updateDownloadStatus(dmId: Long, status: Int, localUri: String?)

    @Query("UPDATE downloads SET bytesDownloaded = :downloaded, totalBytes = :total WHERE downloadManagerId = :dmId")
    suspend fun updateDownloadProgress(dmId: Long, downloaded: Long, total: Long)

    @Query("DELETE FROM downloads WHERE id = :id")
    suspend fun deleteDownload(id: String)

    @Query("DELETE FROM downloads WHERE downloadManagerId = :dmId")
    suspend fun deleteDownloadByDmId(dmId: Long)
}
