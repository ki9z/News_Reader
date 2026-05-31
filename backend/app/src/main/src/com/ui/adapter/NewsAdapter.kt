package com.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.R
import com.databinding.ItemNewsBinding
import com.ui.model.NewsUiModel
import com.util.DateUtils
import com.util.loadImage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NewsAdapter(
    private val onItemClick: (NewsUiModel) -> Unit,
    private val onBookmarkClick: (NewsUiModel) -> Unit = {}
) : ListAdapter<NewsUiModel, NewsAdapter.NewsViewHolder>(DiffCallback()) {

    private fun formatSize(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        if (bytes < 1024 * 1024) return "${bytes / 1024} KB"
        return String.format(Locale.US, "%.1f MB", bytes / (1024f * 1024f))
    }

    private fun formatTimestamp(millis: Long): String {
        val formatter = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
        return formatter.format(Date(millis))
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NewsViewHolder {
        val binding = ItemNewsBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return NewsViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NewsViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class NewsViewHolder(
        private val binding: ItemNewsBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: NewsUiModel) {
            val context = binding.root.context

            binding.tvTitle.text = item.title
            binding.tvSource.text = item.sourceName
            binding.root.contentDescription = item.title
            binding.ivThumbnail.loadImage(item.imageUrl)
            binding.ivThumbnail.contentDescription = context.getString(R.string.article_image_with_title, item.title)

            val bookmarkColor = ContextCompat.getColor(
                context,
                if (item.isBookmarked) R.color.news_chip_selected_bg else R.color.news_secondary_text
            )
            binding.btnBookmark.setColorFilter(bookmarkColor, android.graphics.PorterDuff.Mode.SRC_IN)
            binding.btnBookmark.contentDescription = context.getString(
                if (item.isBookmarked) R.string.home_bookmark_removed else R.string.bookmark
            )

            binding.btnBookmark.setOnClickListener {
                if (!item.hasValidUrl) return@setOnClickListener
                onBookmarkClick(item)
            }

            val extras = mutableListOf<String>()

            item.completionPercent?.let {
                extras.add(context.getString(R.string.news_meta_read_percent, it))
            }

            item.readSeconds?.takeIf { it > 0 }?.let {
                extras.add("${it}s")
            }

            item.status?.takeIf { it.isNotBlank() }?.let {
                extras.add(it.replaceFirstChar { c -> c.uppercase() })
            }

            item.fileSizeBytes?.let {
                extras.add(formatSize(it))
            }

            val timeText = item.eventTimeMillis?.let { formatTimestamp(it) }
                ?: DateUtils.formatPublishedAt(item.publishedAt)

            if (timeText.isNotBlank()) {
                extras.add(timeText)
            }

            val prefix = if (item.canResume) {
                context.getString(R.string.news_meta_resume_prefix)
            } else {
                ""
            }

            binding.tvPublishedAt.text = "$prefix${extras.joinToString(" | ")}"

            binding.root.setOnClickListener {
                if (!item.hasValidUrl) return@setOnClickListener
                onItemClick(item)
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<NewsUiModel>() {
        override fun areItemsTheSame(oldItem: NewsUiModel, newItem: NewsUiModel): Boolean {
            return oldItem.itemId == newItem.itemId && oldItem.articleUrl == newItem.articleUrl
        }

        override fun areContentsTheSame(oldItem: NewsUiModel, newItem: NewsUiModel): Boolean {
            return oldItem == newItem
        }
    }
}