package com.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.R
import com.databinding.ItemLocalHeadlineBinding
import com.ui.model.NewsUiModel
import com.util.DateUtils
import com.util.loadImage

class LocalHeadlineAdapter(
    private val onItemClick: (NewsUiModel) -> Unit,
    private val onBookmarkClick: (NewsUiModel) -> Unit = {}
) : ListAdapter<NewsUiModel, LocalHeadlineAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemLocalHeadlineBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemLocalHeadlineBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: NewsUiModel) {
            binding.tvHeadlineTitle.text = item.title
            binding.tvHeadlineMeta.text = buildString {
                append(item.sourceName)
                val time = DateUtils.formatPublishedAt(item.publishedAt)
                if (time.isNotBlank()) {
                    append(" • ")
                    append(time)
                }
            }
            binding.ivHeadlineImage.loadImage(item.imageUrl)
            val bookmarkColor = ContextCompat.getColor(
                binding.root.context,
                if (item.isBookmarked) R.color.news_chip_selected_bg else R.color.news_secondary_text
            )
            binding.btnHeadlineBookmark.setColorFilter(bookmarkColor, android.graphics.PorterDuff.Mode.SRC_IN)
            binding.btnHeadlineBookmark.setOnClickListener { onBookmarkClick(item) }
            binding.root.setOnClickListener { onItemClick(item) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<NewsUiModel>() {
        override fun areItemsTheSame(oldItem: NewsUiModel, newItem: NewsUiModel): Boolean {
            return oldItem.articleUrl == newItem.articleUrl
        }

        override fun areContentsTheSame(oldItem: NewsUiModel, newItem: NewsUiModel): Boolean = oldItem == newItem
    }
}

