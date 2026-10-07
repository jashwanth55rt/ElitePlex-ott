package com.example.ui.movies

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.R
import com.example.data.model.MovieItem
import com.example.databinding.ItemContentPosterGridBinding

class MovieGridAdapter(
    private val onItemClick: (MovieItem) -> Unit
) : ListAdapter<MovieItem, MovieGridAdapter.GridMovieViewHolder>(MovieDiffCallback) {

    inner class GridMovieViewHolder(private val binding: ItemContentPosterGridBinding) :
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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GridMovieViewHolder {
        val binding = ItemContentPosterGridBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return GridMovieViewHolder(binding)
    }

    override fun onBindViewHolder(holder: GridMovieViewHolder, position: Int) {
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
