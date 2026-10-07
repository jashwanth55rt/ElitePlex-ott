package com.example.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.data.model.StreamingPlatform
import com.example.databinding.ItemStreamingPlatformBinding

class StreamingPlatformAdapter(
    private val platforms: List<StreamingPlatform>,
    private val onPlatformClick: (StreamingPlatform) -> Unit
) : RecyclerView.Adapter<StreamingPlatformAdapter.PlatformViewHolder>() {

    inner class PlatformViewHolder(private val binding: ItemStreamingPlatformBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(platform: StreamingPlatform) {
            binding.ivPlatformLogo.setImageResource(platform.iconResId)
            binding.tvPlatformName.text = platform.name
            binding.root.setOnClickListener {
                onPlatformClick(platform)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlatformViewHolder {
        val binding = ItemStreamingPlatformBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return PlatformViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PlatformViewHolder, position: Int) {
        holder.bind(platforms[position])
    }

    override fun getItemCount(): Int = platforms.size
}
