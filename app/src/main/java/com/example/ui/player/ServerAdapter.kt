package com.example.ui.player

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.R
import com.example.data.model.PlaySource
import com.example.databinding.ItemServerChipBinding

class ServerAdapter(
    private val servers: List<PlaySource>,
    private var selectedIndex: Int = 0,
    private val onServerSelected: (Int, PlaySource) -> Unit
) : RecyclerView.Adapter<ServerAdapter.ServerViewHolder>() {

    fun setSelectedIndex(index: Int) {
        val old = selectedIndex
        selectedIndex = index
        notifyItemChanged(old)
        notifyItemChanged(selectedIndex)
    }

    inner class ServerViewHolder(private val binding: ItemServerChipBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(server: PlaySource, position: Int) {
            val isSelected = position == selectedIndex
            binding.tvServerName.text = server.displayLabel

            val context = binding.root.context
            if (isSelected) {
                binding.tvServerName.setBackgroundResource(R.drawable.bg_button_gold)
                binding.tvServerName.setTextColor(
                    ContextCompat.getColor(context, R.color.accent_gold_dark)
                )
            } else {
                binding.tvServerName.setBackgroundResource(R.drawable.bg_chip)
                binding.tvServerName.setTextColor(
                    ContextCompat.getColor(context, R.color.text_primary)
                )
            }

            binding.root.setOnClickListener {
                if (selectedIndex != position) {
                    setSelectedIndex(position)
                    onServerSelected(position, server)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ServerViewHolder {
        val binding = ItemServerChipBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ServerViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ServerViewHolder, position: Int) {
        holder.bind(servers[position], position)
    }

    override fun getItemCount(): Int = servers.size
}
