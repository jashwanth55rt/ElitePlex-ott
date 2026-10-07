package com.example.utils

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import com.example.ElitePlexApplication
import com.example.data.local.DownloadItemEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

object DownloadHelper {

    fun startDownload(
        context: Context,
        contentId: String,
        title: String,
        subtitle: String? = null,
        poster: String? = null,
        mediaType: String = "movie",
        seasonNumber: Int = 0,
        episodeNumber: Int = 0,
        downloadUrl: String,
        quality: String = "1080p"
    ): Long {
        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

        val safeTitle = title.replace(Regex("[^a-zA-Z0-9.-]"), "_")
        val fileName = if (seasonNumber > 0 && episodeNumber > 0) {
            "${safeTitle}_S${seasonNumber}E${episodeNumber}_${quality}.mp4"
        } else {
            "${safeTitle}_${quality}.mp4"
        }

        val destinationDir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES) ?: context.filesDir
        if (!destinationDir.exists()) {
            destinationDir.mkdirs()
        }
        val destinationFile = File(destinationDir, fileName)

        val uri = Uri.parse(downloadUrl)
        val request = DownloadManager.Request(uri).apply {
            setTitle(title)
            setDescription(subtitle ?: "Downloading $quality for offline playback")
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationInExternalFilesDir(context, Environment.DIRECTORY_MOVIES, fileName)
            setAllowedOverMetered(true)
            setAllowedOverRoaming(true)
        }

        val downloadId = runCatching { dm.enqueue(request) }.getOrElse { -1L }

        val entityId = if (seasonNumber > 0 && episodeNumber > 0) {
            "${contentId}_s${seasonNumber}_e${episodeNumber}"
        } else {
            contentId
        }

        val entity = DownloadItemEntity(
            id = entityId,
            downloadManagerId = downloadId,
            contentId = contentId,
            title = title,
            subtitle = subtitle ?: quality,
            poster = poster,
            mediaType = mediaType,
            seasonNumber = seasonNumber,
            episodeNumber = episodeNumber,
            downloadUrl = downloadUrl,
            localUri = Uri.fromFile(destinationFile).toString(),
            status = if (downloadId > 0) DownloadManager.STATUS_RUNNING else DownloadManager.STATUS_FAILED,
            quality = quality
        )

        val app = context.applicationContext as ElitePlexApplication
        CoroutineScope(Dispatchers.IO).launch {
            app.database.downloadDao().insertDownload(entity)
        }

        val msg = if (downloadId > 0) {
            "Downloading $title ($quality) for offline viewing"
        } else {
            "Failed to start download"
        }
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        return downloadId
    }

    suspend fun syncDownloads(context: Context) {
        val app = context.applicationContext as ElitePlexApplication
        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val list = app.database.downloadDao().getAllDownloadsList()

        for (item in list) {
            if (item.status == DownloadManager.STATUS_RUNNING || item.status == DownloadManager.STATUS_PENDING) {
                if (item.downloadManagerId > 0) {
                    val query = DownloadManager.Query().setFilterById(item.downloadManagerId)
                    val cursor = runCatching { dm.query(query) }.getOrNull()
                    if (cursor != null && cursor.moveToFirst()) {
                        val statusIdx = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                        val downIdx = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                        val totalIdx = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                        val uriIdx = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)

                        val status = if (statusIdx != -1) cursor.getInt(statusIdx) else item.status
                        val downloaded = if (downIdx != -1) cursor.getLong(downIdx) else item.bytesDownloaded
                        val total = if (totalIdx != -1) cursor.getLong(totalIdx) else item.totalBytes
                        val localUri = if (uriIdx != -1) cursor.getString(uriIdx) else null

                        cursor.close()

                        app.database.downloadDao().updateDownloadStatus(
                            item.downloadManagerId,
                            status,
                            localUri ?: item.localUri
                        )
                        if (total > 0) {
                            app.database.downloadDao().updateDownloadProgress(
                                item.downloadManagerId,
                                downloaded,
                                total
                            )
                        }
                    } else {
                        cursor?.close()
                        // Fallback: check if destination file exists on disk
                        item.localUri?.let { u ->
                            val path = Uri.parse(u).path
                            if (path != null) {
                                val file = File(path)
                                if (file.exists() && file.length() > 0) {
                                    app.database.downloadDao().updateDownloadStatus(
                                        item.downloadManagerId,
                                        DownloadManager.STATUS_SUCCESSFUL,
                                        u
                                    )
                                    app.database.downloadDao().updateDownloadProgress(
                                        item.downloadManagerId,
                                        file.length(),
                                        file.length()
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    fun cancelOrDeleteDownload(context: Context, entity: DownloadItemEntity) {
        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        if (entity.downloadManagerId > 0) {
            runCatching { dm.remove(entity.downloadManagerId) }
        }

        entity.localUri?.let { uriStr ->
            runCatching {
                val uri = Uri.parse(uriStr)
                uri.path?.let { File(it).delete() }
            }
        }

        val app = context.applicationContext as ElitePlexApplication
        CoroutineScope(Dispatchers.IO).launch {
            app.database.downloadDao().deleteDownload(entity.id)
        }

        Toast.makeText(context, "Deleted from offline downloads", Toast.LENGTH_SHORT).show()
    }
}
