package com.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.R
import com.databinding.ItemHomeNewsBinding
import com.ui.model.NewsUiModel
import com.util.DateUtils
import com.util.loadImage

class HomeNewsAdapter(
    private val onItemClick: (NewsUiModel) -> Unit,
    private val onBookmarkClick: (NewsUiModel) -> Unit,
    private val onItemLongClick: (NewsUiModel) -> Unit
) : ListAdapter<NewsUiModel, HomeNewsAdapter.HomeNewsViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HomeNewsViewHolder {
        val binding = ItemHomeNewsBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return HomeNewsViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HomeNewsViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class HomeNewsViewHolder(
        private val binding: ItemHomeNewsBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: NewsUiModel) {
            val context = binding.root.context
            binding.tvTitle.text = item.title
            binding.root.contentDescription = item.title

            val formattedTime = DateUtils.formatPublishedAt(item.publishedAt)
            binding.tvMeta.text = if (formattedTime.isBlank()) {
                item.sourceName
            } else {
                "${item.sourceName} • $formattedTime"
            }

            binding.ivThumbnail.loadImage(item.imageUrl)
            binding.ivThumbnail.contentDescription = context.getString(R.string.article_image_with_title, item.title)

            binding.root.setOnClickListener {
                if (!item.hasValidUrl) return@setOnClickListener
                onItemClick(item)
            }

            binding.root.setOnLongClickListener {
                if (!item.hasValidUrl) return@setOnLongClickListener true
                onItemLongClick(item)
                true
            }

            binding.btnBookmark.apply {
                isSelected = item.isBookmarked
                contentDescription = context.getString(
                    if (item.isBookmarked) R.string.home_bookmark_removed else R.string.bookmark
                )
                imageTintList = android.content.res.ColorStateList.valueOf(
                    ContextCompat.getColor(
                        context,
                        if (item.isBookmarked) R.color.news_link_blue else R.color.white
                    )
                )

                setOnClickListener {
                    if (!item.hasValidUrl) return@setOnClickListener
                    onBookmarkClick(item)
                }
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<NewsUiModel>() {
        override fun areItemsTheSame(oldItem: NewsUiModel, newItem: NewsUiModel): Boolean {
            return oldItem.articleUrl == newItem.articleUrl &&
                    oldItem.title == newItem.title &&
                    oldItem.sourceName == newItem.sourceName
        }

        override fun areContentsTheSame(oldItem: NewsUiModel, newItem: NewsUiModel): Boolean {
            return oldItem == newItem
        }
    }
}