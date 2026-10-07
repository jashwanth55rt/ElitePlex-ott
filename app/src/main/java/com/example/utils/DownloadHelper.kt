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
        val destinationFile = File(destinationDir, fileName)

        val uri = Uri.parse(downloadUrl)
        val request = DownloadManager.Request(uri).apply {
            setTitle(title)
            setDescription(subtitle ?: "Downloading $quality")
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationUri(Uri.fromFile(destinationFile))
            setAllowedOverMetered(true)
            setAllowedOverRoaming(true)
        }

        val downloadId = dm.enqueue(request)

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
            status = DownloadManager.STATUS_RUNNING,
            quality = quality
        )

        val app = context.applicationContext as ElitePlexApplication
        CoroutineScope(Dispatchers.IO).launch {
            app.database.downloadDao().insertDownload(entity)
        }

        Toast.makeText(context, "Download started: $title ($quality)", Toast.LENGTH_SHORT).show()
        return downloadId
    }

    fun cancelOrDeleteDownload(context: Context, entity: DownloadItemEntity) {
        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        dm.remove(entity.downloadManagerId)

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

        Toast.makeText(context, "Download removed: ${entity.title}", Toast.LENGTH_SHORT).show()
    }
}
