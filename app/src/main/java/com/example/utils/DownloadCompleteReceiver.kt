package com.example.utils

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.ElitePlexApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DownloadCompleteReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == DownloadManager.ACTION_DOWNLOAD_COMPLETE) {
            val downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
            if (downloadId == -1L) return

            val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val query = DownloadManager.Query().setFilterById(downloadId)
            val cursor = dm.query(query)

            if (cursor != null && cursor.moveToFirst()) {
                val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                val uriIndex = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)
                val totalBytesIndex = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                val bytesDownloadedIndex = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)

                val status = if (statusIndex != -1) cursor.getInt(statusIndex) else DownloadManager.STATUS_SUCCESSFUL
                val localUri = if (uriIndex != -1) cursor.getString(uriIndex) else null
                val totalBytes = if (totalBytesIndex != -1) cursor.getLong(totalBytesIndex) else 0L
                val bytesDownloaded = if (bytesDownloadedIndex != -1) cursor.getLong(bytesDownloadedIndex) else totalBytes

                cursor.close()

                val app = context.applicationContext as ElitePlexApplication
                CoroutineScope(Dispatchers.IO).launch {
                    app.database.downloadDao().updateDownloadStatus(downloadId, status, localUri)
                    if (totalBytes > 0) {
                        app.database.downloadDao().updateDownloadProgress(downloadId, bytesDownloaded, totalBytes)
                    }
                }
            } else {
                cursor?.close()
            }
        }
    }
}
