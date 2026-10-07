package com.example.ui.details

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.R
import com.example.data.model.Episode
import com.example.databinding.ItemEpisodeBinding

class EpisodesAdapter(
    private val onEpisodeClick: (Episode) -> Unit,
    private val onDownloadClick: (Episode) -> Unit
) : ListAdapter<Episode, EpisodesAdapter.EpisodeViewHolder>(EpisodeDiffCallback) {

    inner class EpisodeViewHolder(private val binding: ItemEpisodeBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Episode) {
            val num = item.episodeNumber ?: (adapterPosition + 1)
            binding.tvEpisodeNumberTitle.text = "$num. ${item.name ?: "Episode $num"}"
            binding.tvEpisodeOverview.text = item.overview?.takeIf { it.isNotBlank() }
                ?: "Select to stream this episode."

            binding.ivEpisodeThumbnail.load(item.still) {
                crossfade(true)
                placeholder(R.drawable.bg_poster_placeholder)
                error(R.drawable.bg_poster_placeholder)
            }

            binding.btnDownloadEpisode.setOnClickListener {
                onDownloadClick(item)
            }

            binding.root.setOnClickListener {
                onEpisodeClick(item)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EpisodeViewHolder {
        val binding = ItemEpisodeBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return EpisodeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: EpisodeViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object EpisodeDiffCallback : DiffUtil.ItemCallback<Episode>() {
        override fun areItemsTheSame(oldItem: Episode, newItem: Episode): Boolean {
            return oldItem.episodeNumber == newItem.episodeNumber
        }

        override fun areContentsTheSame(oldItem: Episode, newItem: Episode): Boolean {
            return oldItem == newItem
        }
    }
}
