package com.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.R
import com.databinding.ItemTrendingBinding
import com.ui.model.TrendingItem
import com.util.loadImage

class TrendingAdapter(
    private val onItemClick: (TrendingItem) -> Unit
) : ListAdapter<TrendingItem, TrendingAdapter.TrendingViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrendingViewHolder {
        val binding = ItemTrendingBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TrendingViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TrendingViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TrendingViewHolder(
        private val binding: ItemTrendingBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: TrendingItem) {
            binding.tvRank.text = item.rank.toString()
            binding.tvTitle.text = item.title
            binding.tvDescription.text = item.description
            binding.ivThumbnail.loadImage(item.thumbnailUrl)
            binding.tvRank.setTextColor(
                ContextCompat.getColor(binding.root.context, rankColor(item.rank))
            )
            binding.root.setOnClickListener { onItemClick(item) }
        }

        private fun rankColor(rank: Int): Int = when (rank % 4) {
            1 -> R.color.news_trending_rank_1
            2 -> R.color.news_trending_rank_2
            3 -> R.color.news_trending_rank_3
            else -> R.color.news_trending_rank_4
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<TrendingItem>() {
        override fun areItemsTheSame(oldItem: TrendingItem, newItem: TrendingItem): Boolean =
            oldItem.rank == newItem.rank

        override fun areContentsTheSame(oldItem: TrendingItem, newItem: TrendingItem): Boolean =
            oldItem == newItem
    }
}

