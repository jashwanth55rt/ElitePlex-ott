package com.example.ui.downloads

import android.app.DownloadManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.R
import com.example.data.local.DownloadItemEntity
import com.example.databinding.ItemDownloadCardBinding

class DownloadsAdapter(
    private val onPlayOfflineClick: (DownloadItemEntity) -> Unit,
    private val onDeleteClick: (DownloadItemEntity) -> Unit
) : ListAdapter<DownloadItemEntity, DownloadsAdapter.DownloadViewHolder>(DownloadDiffCallback) {

    inner class DownloadViewHolder(private val binding: ItemDownloadCardBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: DownloadItemEntity) {
            binding.tvDownloadTitle.text = item.title
            binding.tvDownloadSubtitle.text = item.subtitle ?: "${item.quality} • ${item.formattedSize}"

            val context = binding.root.context
            when (item.status) {
                DownloadManager.STATUS_SUCCESSFUL -> {
                    binding.tvDownloadStatus.text = "OFFLINE READY"
                    binding.tvDownloadStatus.setTextColor(ContextCompat.getColor(context, R.color.accent_teal))
                    binding.pbDownloadProgress.visibility = View.GONE
                    binding.btnPlayOffline.visibility = View.VISIBLE
                }
                DownloadManager.STATUS_RUNNING -> {
                    binding.tvDownloadStatus.text = "DOWNLOADING ${item.progressPercent}%"
                    binding.tvDownloadStatus.setTextColor(ContextCompat.getColor(context, R.color.accent_gold))
                    binding.pbDownloadProgress.visibility = View.VISIBLE
                    binding.pbDownloadProgress.progress = item.progressPercent
                    binding.btnPlayOffline.visibility = View.GONE
                }
                DownloadManager.STATUS_PENDING -> {
                    binding.tvDownloadStatus.text = "WAITING..."
                    binding.tvDownloadStatus.setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
                    binding.pbDownloadProgress.visibility = View.VISIBLE
                    binding.pbDownloadProgress.isIndeterminate = true
                    binding.btnPlayOffline.visibility = View.GONE
                }
                else -> {
                    binding.tvDownloadStatus.text = "FAILED"
                    binding.tvDownloadStatus.setTextColor(ContextCompat.getColor(context, R.color.accent_hot))
                    binding.pbDownloadProgress.visibility = View.GONE
                    binding.btnPlayOffline.visibility = View.GONE
                }
            }

            binding.ivDownloadPoster.load(item.poster) {
                crossfade(true)
                placeholder(R.drawable.bg_poster_placeholder)
                error(R.drawable.bg_poster_placeholder)
            }

            binding.btnPlayOffline.setOnClickListener {
                onPlayOfflineClick(item)
            }

            binding.btnDeleteDownload.setOnClickListener {
                onDeleteClick(item)
            }

            binding.root.setOnClickListener {
                if (item.isCompleted) {
                    onPlayOfflineClick(item)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DownloadViewHolder {
        val binding = ItemDownloadCardBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return DownloadViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DownloadViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object DownloadDiffCallback : DiffUtil.ItemCallback<DownloadItemEntity>() {
        override fun areItemsTheSame(
            oldItem: DownloadItemEntity,
            newItem: DownloadItemEntity
        ): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(
            oldItem: DownloadItemEntity,
            newItem: DownloadItemEntity
        ): Boolean {
            return oldItem == newItem
        }
    }
}
