package com.example.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.R
import com.example.data.model.MovieItem
import com.example.databinding.ItemContentPosterBinding

class HorizontalMovieAdapter(
    private val onItemClick: (MovieItem) -> Unit
) : ListAdapter<MovieItem, HorizontalMovieAdapter.MovieViewHolder>(MovieDiffCallback) {

    inner class MovieViewHolder(private val binding: ItemContentPosterBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: MovieItem) {
            binding.tvTitle.text = item.displayTitle
            binding.tvYear.text = item.year ?: ""
            binding.tvRating.text = item.formattedRating
            binding.tvBadgeType.text = if (item.isTvSeries) "TV" else "MOVIE"

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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MovieViewHolder {
        val binding = ItemContentPosterBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return MovieViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MovieViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object MovieDiffCallback : DiffUtil.ItemCallback<MovieItem>() {
        override fun areItemsTheSame(oldItem: MovieItem, newItem: MovieItem): Boolean {
            return oldItem.displayId == newItem.displayId
        }

        override fun areContentsTheSame(oldItem: MovieItem, newItem: MovieItem): Boolean {
            return oldItem == newItem
        }
    }
}
