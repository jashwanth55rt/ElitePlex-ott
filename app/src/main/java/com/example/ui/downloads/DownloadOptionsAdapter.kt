package com.example.ui.downloads

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.data.model.DownloadOption
import com.example.databinding.ItemDownloadOptionBinding

class DownloadOptionsAdapter(
    private val options: List<DownloadOption>,
    private val onOptionClick: (DownloadOption) -> Unit
) : RecyclerView.Adapter<DownloadOptionsAdapter.OptionViewHolder>() {

    inner class OptionViewHolder(private val binding: ItemDownloadOptionBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(option: DownloadOption) {
            binding.tvOptionLabel.text = option.label
            binding.tvOptionSize.text = option.sizeText ?: "High Quality Video"

            binding.root.setOnClickListener {
                onOptionClick(option)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OptionViewHolder {
        val binding = ItemDownloadOptionBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return OptionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: OptionViewHolder, position: Int) {
        holder.bind(options[position])
    }

    override fun getItemCount(): Int = options.size
}
