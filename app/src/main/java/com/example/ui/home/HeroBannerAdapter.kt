package com.example.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.size.Scale
import com.example.R
import com.example.data.model.MovieItem
import com.example.databinding.ItemHeroSlideBinding

class HeroBannerAdapter(
    private val items: List<MovieItem>,
    private val onWatchClick: (MovieItem) -> Unit,
    private val onDetailsClick: (MovieItem) -> Unit,
    private val onBookmarkClick: (MovieItem) -> Unit = {}
) : RecyclerView.Adapter<HeroBannerAdapter.HeroViewHolder>() {

    inner class HeroViewHolder(private val binding: ItemHeroSlideBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: MovieItem) {
            val imageSource = item.resolvedBackdrop ?: item.resolvedPoster
            binding.ivHeroBackdrop.load(imageSource) {
                crossfade(true)
                scale(Scale.FILL)
                placeholder(R.drawable.bg_poster_placeholder)
                error(R.drawable.bg_poster_placeholder)
            }

            binding.tvHeroTitle.text = item.displayTitle
            binding.tvHeroYear.text = item.year ?: "2026"
            binding.tvHeroRating.text = "★ ${item.formattedRating}"

            val subtitleText = if (item.isTvSeries) {
                "${item.year ?: "2026"} • TV Series"
            } else {
                "${item.year ?: "2026"} • Feature Film"
            }
            binding.tvHeroSubtitle.text = subtitleText
            binding.tvHeroBadge.text = if (item.rating != null && item.rating >= 8.0) "Top Rated" else "Featured"

            binding.btnHeroWatch.setOnClickListener { onWatchClick(item) }
            binding.btnHeroDetails.setOnClickListener { onDetailsClick(item) }
            binding.btnHeroBookmark.setOnClickListener { onBookmarkClick(item) }
            binding.root.setOnClickListener { onDetailsClick(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HeroViewHolder {
        val binding = ItemHeroSlideBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        binding.root.layoutParams = RecyclerView.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        return HeroViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HeroViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size
}
