package com.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.R
import com.databinding.ItemExploreTopicBinding
import com.ui.model.ExploreTopicItem
import com.util.loadImage

class ExploreTopicAdapter(
    private val onItemClick: (ExploreTopicItem) -> Unit
) : ListAdapter<ExploreTopicItem, ExploreTopicAdapter.ExploreTopicViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExploreTopicViewHolder {
        val binding = ItemExploreTopicBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ExploreTopicViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ExploreTopicViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ExploreTopicViewHolder(
        private val binding: ItemExploreTopicBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ExploreTopicItem) {
            val context = binding.root.context
            binding.ivTopic.loadImage(item.imageUrl)
            binding.tvTopicTitle.text = when (item.id) {
                "us-politics" -> context.getString(R.string.search_explore_topic_us_politics)
                "foreign-policy" -> context.getString(R.string.search_explore_topic_foreign_policy)
                "middle-east" -> context.getString(R.string.search_explore_topic_middle_east)
                "entertainment" -> context.getString(R.string.search_explore_topic_entertainment)
                "movies" -> context.getString(R.string.search_explore_topic_movies)
                "technology" -> context.getString(R.string.search_explore_topic_technology)
                "science" -> context.getString(R.string.search_explore_topic_science)
                "business" -> context.getString(R.string.search_explore_topic_business)
                "health" -> context.getString(R.string.search_explore_topic_health)
                "sports" -> context.getString(R.string.search_explore_topic_sports)
                else -> item.title
            }
            binding.root.setOnClickListener { onItemClick(item) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<ExploreTopicItem>() {
        override fun areItemsTheSame(oldItem: ExploreTopicItem, newItem: ExploreTopicItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ExploreTopicItem, newItem: ExploreTopicItem): Boolean {
            return oldItem == newItem
        }
    }
}

