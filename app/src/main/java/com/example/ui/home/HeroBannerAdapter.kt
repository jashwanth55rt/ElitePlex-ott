package com.example.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
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
            val imageSource = item.backdrop ?: item.poster
            binding.ivHeroBackdrop.load(imageSource) {
                crossfade(true)
                placeholder(R.drawable.bg_poster_placeholder)
                error(R.drawable.bg_poster_placeholder)
            }

            binding.tvHeroTitle.text = item.displayTitle
            binding.tvHeroYear.text = item.year ?: "2026"
            binding.tvHeroRating.text = item.formattedRating
            binding.tvHeroOverview.text = item.overview ?: "Stream this featured title on ElitePlex."

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
