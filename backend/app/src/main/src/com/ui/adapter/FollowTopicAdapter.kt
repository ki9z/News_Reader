package com.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.R
import com.databinding.ItemFollowTopicBinding
import com.ui.model.FollowTopicUiModel

class FollowTopicAdapter(
    private val onToggle: (FollowTopicUiModel) -> Unit,
    private val onMute: (FollowTopicUiModel) -> Unit,
    private val onBlock: (FollowTopicUiModel) -> Unit
) : ListAdapter<FollowTopicUiModel, FollowTopicAdapter.TopicViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TopicViewHolder {
        val binding = ItemFollowTopicBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TopicViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TopicViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TopicViewHolder(
        private val binding: ItemFollowTopicBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: FollowTopicUiModel) {
            val context = binding.root.context
            binding.tvTopicName.text = localizedName(item)
            val meta = context.getString(R.string.profile_topic_meta_template, item.newTodayCount)
            binding.tvMeta.text = if (item.muted) {
                "$meta | ${context.getString(R.string.profile_topic_meta_muted)}"
            } else {
                meta
            }
            binding.switchFollow.setOnCheckedChangeListener(null)
            binding.switchFollow.isChecked = item.isFollowed
            binding.switchFollow.setOnCheckedChangeListener { _, _ ->
                onToggle(item)
            }
            binding.btnMute.text = if (item.muted) {
                context.getString(R.string.profile_unmute_topic)
            } else {
                context.getString(R.string.profile_mute_topic)
            }
            binding.root.setOnClickListener {
                onToggle(item)
            }
            binding.btnMute.setOnClickListener { onMute(item) }
            binding.btnBlock.setOnClickListener { onBlock(item) }
        }

        private fun localizedName(item: FollowTopicUiModel): String {
            val context = binding.root.context
            val mappedRes = when (item.id) {
                "top_news" -> R.string.follow_topic_top_news
                "politics" -> R.string.follow_topic_politics
                "business" -> R.string.follow_topic_business
                "technology" -> R.string.follow_topic_technology
                "sports" -> R.string.follow_topic_sports
                "entertainment" -> R.string.follow_topic_entertainment
                "health" -> R.string.follow_topic_health
                "science" -> R.string.follow_topic_science
                "travel" -> R.string.follow_topic_travel
                "lifestyle" -> R.string.follow_topic_lifestyle
                "kw_startup" -> R.string.follow_topic_keyword_startup
                "kw_education" -> R.string.follow_topic_keyword_education
                else -> null
            }
            return mappedRes?.let(context::getString) ?: item.name
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<FollowTopicUiModel>() {
        override fun areItemsTheSame(oldItem: FollowTopicUiModel, newItem: FollowTopicUiModel): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: FollowTopicUiModel, newItem: FollowTopicUiModel): Boolean {
            return oldItem == newItem
        }
    }
}

