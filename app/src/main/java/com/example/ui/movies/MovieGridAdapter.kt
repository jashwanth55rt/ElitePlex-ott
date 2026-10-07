package com.example.ui.movies

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.size.Scale
import com.example.R
import com.example.data.model.MovieItem
import com.example.databinding.ItemContentPosterGridBinding
import com.example.utils.ImageUtils

class MovieGridAdapter(
    private val onItemClick: (MovieItem) -> Unit
) : ListAdapter<MovieItem, MovieGridAdapter.GridMovieViewHolder>(MovieDiffCallback) {

    inner class GridMovieViewHolder(private val binding: ItemContentPosterGridBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: MovieItem) {
            binding.tvTitle.text = item.displayTitle
            binding.tvRating.text = "★ ${item.formattedRating}"

            if (item.isTvSeries) {
                binding.tvBadgeType.visibility = View.VISIBLE
                binding.tvBadgeType.text = "TV"
                binding.tvYear.text = "Series"
            } else {
                binding.tvBadgeType.visibility = View.GONE
                binding.tvYear.text = "Movie"
            }

            val posterUrl = item.resolvedPoster ?: item.resolvedBackdrop
            binding.ivPoster.load(posterUrl) {
                crossfade(true)
                scale(Scale.FILL)
                placeholder(R.drawable.bg_poster_placeholder)
                error(R.drawable.bg_poster_placeholder)
            }

            binding.root.setOnClickListener {
                onItemClick(item)
            }

            binding.ivCardPlay.setOnClickListener {
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
