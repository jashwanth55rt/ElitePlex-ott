package com.example.ui.mylist

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.R
import com.example.data.local.SavedItemEntity
import com.example.databinding.ItemContentPosterGridBinding

class MyListAdapter(
    private val onItemClick: (SavedItemEntity) -> Unit
) : ListAdapter<SavedItemEntity, MyListAdapter.SavedViewHolder>(SavedDiffCallback) {

    inner class SavedViewHolder(private val binding: ItemContentPosterGridBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: SavedItemEntity) {
            binding.tvTitle.text = item.title
            binding.tvYear.text = item.year ?: ""
            binding.tvRating.text = item.rating?.let { String.format("%.1f", it) } ?: "N/A"
            binding.tvBadgeType.text = if (item.type.equals("tv", ignoreCase = true)) "TV" else "MOVIE"

            val poster = item.poster ?: item.backdrop
            binding.ivPoster.load(poster) {
                crossfade(true)
                placeholder(R.drawable.bg_poster_placeholder)
                error(R.drawable.bg_poster_placeholder)
            }

            binding.root.setOnClickListener {
                onItemClick(item)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SavedViewHolder {
        val binding = ItemContentPosterGridBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return SavedViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SavedViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object SavedDiffCallback : DiffUtil.ItemCallback<SavedItemEntity>() {
        override fun areItemsTheSame(oldItem: SavedItemEntity, newItem: SavedItemEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: SavedItemEntity, newItem: SavedItemEntity): Boolean {
            return oldItem == newItem
        }
    }
}
