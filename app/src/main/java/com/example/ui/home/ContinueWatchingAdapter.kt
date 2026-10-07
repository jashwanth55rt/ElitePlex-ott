package com.example.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.R
import com.example.data.local.WatchHistoryEntity
import com.example.databinding.ItemContinueWatchingBinding

class ContinueWatchingAdapter(
    private val onItemClick: (WatchHistoryEntity) -> Unit
) : ListAdapter<WatchHistoryEntity, ContinueWatchingAdapter.HistoryViewHolder>(HistoryDiffCallback) {

    inner class HistoryViewHolder(private val binding: ItemContinueWatchingBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: WatchHistoryEntity) {
            binding.tvCwTitle.text = item.title
            binding.tvCwInfo.text = if (item.type.equals("tv", ignoreCase = true)) {
                "S${item.seasonNumber} • E${item.episodeNumber}"
            } else {
                "Resume Playback"
            }

            binding.pbCwProgress.progress = item.progressPercentage

            val image = item.backdrop ?: item.poster
            binding.ivCwBackdrop.load(image) {
                crossfade(true)
                placeholder(R.drawable.bg_poster_placeholder)
                error(R.drawable.bg_poster_placeholder)
            }

            binding.root.setOnClickListener {
                onItemClick(item)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val binding = ItemContinueWatchingBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return HistoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object HistoryDiffCallback : DiffUtil.ItemCallback<WatchHistoryEntity>() {
        override fun areItemsTheSame(
            oldItem: WatchHistoryEntity,
            newItem: WatchHistoryEntity
        ): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(
            oldItem: WatchHistoryEntity,
            newItem: WatchHistoryEntity
        ): Boolean {
            return oldItem == newItem
        }
    }
}
