package com.ui.detail

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.NewsApp
import com.R
import com.data.model.Article
import com.data.model.Source
import com.databinding.FragmentDetailBinding
import com.ui.adapter.NewsAdapter
import com.ui.model.NewsUiModel
import com.util.UiState
import com.util.gone
import com.util.loadImage
import com.util.toDetailBundle
import com.util.visible
import com.viewmodel.detail.DetailViewModel
import com.viewmodel.detail.DetailViewModelFactory
import kotlinx.coroutines.launch

class DetailFragment : Fragment(R.layout.fragment_detail) {

    private var _binding: FragmentDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DetailViewModel by viewModels {
        val app = requireActivity().application as NewsApp
        DetailViewModelFactory(
            repository = app.repository,
            profileRepository = app.profileRepository,
            userSettingsRepository = app.userSettingsRepository
        )
    }

    private lateinit var relatedAdapter: NewsAdapter
    private var openedAtMillis: Long = 0L
    private var hasFinishedReading = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentDetailBinding.bind(view)
        openedAtMillis = System.currentTimeMillis()

        val args = requireArguments()
        val title = args.getString("title").orEmpty()
        val description = args.getString("description").orEmpty()
        val content = args.getString("content").orEmpty()
        val imageUrl = args.getString("imageUrl").orEmpty()
        val articleUrl = args.getString("articleUrl").orEmpty()
        val sourceName = args.getString("sourceName").orEmpty()
        val author = args.getString("author").orEmpty()
        val publishedAt = args.getString("publishedAt").orEmpty()
        val fromScreen = args.getString("fromScreen").orEmpty()

        val article = Article(
            source = Source(id = null, name = sourceName),
            author = author.ifBlank { null },
            title = title,
            description = description.ifBlank { null },
            url = articleUrl,
            urlToImage = imageUrl.ifBlank { null },
            publishedAt = publishedAt.ifBlank { null },
            content = content.ifBlank { description.ifBlank { null } }
        )

        bindArticle(article, title, description, content, imageUrl, sourceName, author, publishedAt)
        setupToolbar()
        setupBackHandler()
        setupActions(article, articleUrl, title)
        setupRelatedArticles()
        observeUi()

        if (articleUrl.isNotBlank()) {
            viewModel.checkBookmarkStatus(articleUrl)
            viewModel.recordReading(article, fromScreen)
        }
        viewModel.loadRelatedArticles(article)
    }

    private fun bindArticle(
        article: Article,
        title: String,
        description: String,
        content: String,
        imageUrl: String,
        sourceName: String,
        author: String,
        publishedAt: String
    ) {
        binding.ivCover.loadImage(imageUrl.ifBlank { null })
        binding.ivCover.contentDescription = getString(R.string.article_image_with_title, title.ifBlank { getString(R.string.article) })
        binding.tvTitle.text = title
        binding.tvDescription.text = description.ifBlank { getString(R.string.content_not_available) }
        binding.tvContent.text = content.ifBlank { article.description ?: getString(R.string.content_not_available) }
        binding.tvSource.text = sourceName.ifBlank { getString(R.string.unknown_source) }
        binding.tvAuthor.text = author.ifBlank { getString(R.string.unknown_author) }
        binding.tvPublishedAt.text = publishedAt
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finishReadingIfNeeded()
            findNavController().popBackStack()
        }
    }

    private fun setupBackHandler() {
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    finishReadingIfNeeded()
                    findNavController().popBackStack()
                }
            }
        )
    }

    private fun setupActions(article: Article, articleUrl: String, title: String) {
        binding.btnOpenArticle.isEnabled = articleUrl.isNotBlank()
        binding.btnOpenArticle.setOnClickListener {
            openArticleInApp(article)
        }

        binding.btnShare.setOnClickListener {
            shareArticle(title, articleUrl)
        }

        binding.btnDownload.setOnClickListener {
            viewModel.downloadArticle(article)
        }

        binding.fabBookmark.setOnClickListener {
            viewModel.toggleBookmark(articleUrl, article)
        }
    }

    private fun setupRelatedArticles() {
        relatedAdapter = NewsAdapter(::openRelatedArticle)
        binding.recyclerRelatedArticles.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = relatedAdapter
            isNestedScrollingEnabled = false
        }
    }

    private fun observeUi() {
        observeBookmarkState()
        observeDownloadState()
        observeSettings()
        observeRelatedArticles()
    }

    private fun observeBookmarkState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.isBookmarked.collect { isBookmarked ->
                    binding.fabBookmark.setImageResource(
                        if (isBookmarked) android.R.drawable.btn_star_big_on
                        else android.R.drawable.btn_star_big_off
                    )
                    binding.fabBookmark.contentDescription = getString(
                        if (isBookmarked) R.string.home_bookmark_removed else R.string.bookmark
                    )
                    binding.fabBookmark.imageTintList = ColorStateList.valueOf(
                        ContextCompat.getColor(
                            requireContext(),
                            if (isBookmarked) R.color.news_chip_selected_bg else R.color.news_secondary_text
                        )
                    )
                }
            }
        }
    }

    private fun observeDownloadState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.downloadState.collect { state ->
                    binding.btnDownload.isEnabled = state !is UiState.Loading
                    binding.btnDownload.text = when (state) {
                        is UiState.Loading -> getString(R.string.profile_download_saving)
                        is UiState.Success -> getString(R.string.profile_download_saved)
                        else -> getString(R.string.profile_download_for_offline)
                    }

                    when (state) {
                        is UiState.Success -> {
                            Toast.makeText(requireContext(), R.string.profile_download_saved, Toast.LENGTH_SHORT).show()
                            viewModel.consumeDownloadMessage()
                        }
                        is UiState.Error -> {
                            val messageRes = if (state.message == "invalid_download") {
                                R.string.profile_download_invalid
                            } else {
                                R.string.profile_download_failed
                            }
                            Toast.makeText(requireContext(), messageRes, Toast.LENGTH_SHORT).show()
                            viewModel.consumeDownloadMessage()
                        }
                        else -> Unit
                    }
                }
            }
        }
    }

    private fun observeSettings() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.settings.collect { settings ->
                    val multiplier = when (settings.textSize.uppercase()) {
                        "S" -> 0.92f
                        "L" -> 1.14f
                        "XL" -> 1.28f
                        else -> 1f
                    }
                    binding.tvTitle.textSize = 20f * multiplier
                    binding.tvDescription.textSize = 16f * multiplier
                    binding.tvContent.textSize = 15f * multiplier
                    binding.tvSource.textSize = 12f * multiplier
                    binding.tvAuthor.textSize = 12f * multiplier
                    binding.tvPublishedAt.textSize = 12f * multiplier
                }
            }
        }
    }

    private fun observeRelatedArticles() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.relatedArticles.collect { state ->
                    binding.progressRelated.gone()
                    binding.tvRelatedStatus.gone()
                    binding.recyclerRelatedArticles.gone()

                    when (state) {
                        is UiState.Loading -> binding.progressRelated.visible()
                        is UiState.Success -> {
                            binding.recyclerRelatedArticles.visible()
                            relatedAdapter.submitList(state.data)
                        }
                        is UiState.Empty -> {
                            binding.tvRelatedStatus.visible()
                            binding.tvRelatedStatus.text = getString(R.string.related_articles_empty)
                        }
                        is UiState.Error -> {
                            binding.tvRelatedStatus.visible()
                            binding.tvRelatedStatus.text = state.message
                        }
                        is UiState.Idle -> Unit
                    }
                }
            }
        }
    }

    private fun openArticleInApp(article: Article) {
        val safeUrl = article.url.orEmpty().trim()
        if (safeUrl.isBlank()) {
            Toast.makeText(requireContext(), R.string.profile_download_invalid, Toast.LENGTH_SHORT).show()
            return
        }
        if (findNavController().currentDestination?.id != R.id.detailFragment) return

        finishReadingIfNeeded()
        findNavController().navigate(
            R.id.action_detailFragment_to_articleWebViewFragment,
            bundleOf(
                "title" to article.title.orEmpty(),
                "articleUrl" to safeUrl,
                "description" to article.description.orEmpty(),
                "content" to article.content.orEmpty(),
                "sourceName" to article.source?.name.orEmpty(),
                "publishedAt" to article.publishedAt.orEmpty(),
                "imageUrl" to article.urlToImage.orEmpty(),
                "startInReaderMode" to viewModel.settings.value.articleStyle.equals("reader", ignoreCase = true)
            )
        )
    }

    private fun shareArticle(title: String, articleUrl: String) {
        val shareText = listOf(title, articleUrl).filter { it.isNotBlank() }.joinToString("\n")
        if (shareText.isBlank()) return

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        startActivity(Intent.createChooser(intent, getString(R.string.home_action_share)))
    }

    private fun openRelatedArticle(item: NewsUiModel) {
        val safeUrl = item.articleUrl.orEmpty().trim()
        if (safeUrl.isBlank()) return
        if (findNavController().currentDestination?.id != R.id.detailFragment) return

        finishReadingIfNeeded()
        findNavController().navigate(
            R.id.action_detailFragment_self,
            item.toDetailBundle("related")
        )
    }

    private fun finishReadingIfNeeded() {
        if (hasFinishedReading) return
        hasFinishedReading = true

        val readSeconds = ((System.currentTimeMillis() - openedAtMillis) / 1000L).toInt().coerceAtLeast(1)
        val completionPercent = estimateCompletionPercent()
        viewModel.finishReading(readSeconds, completionPercent)
    }

    private fun estimateCompletionPercent(): Int {
        val contentHeight = binding.scrollArticleContent.getChildAt(0)?.height ?: return 1
        val viewportHeight = binding.scrollArticleContent.height
        val scrollY = binding.scrollArticleContent.scrollY
        val maxScroll = (contentHeight - viewportHeight).coerceAtLeast(1)
        return ((scrollY.toFloat() / maxScroll.toFloat()) * 100f).toInt().coerceIn(1, 100)
    }

    override fun onStop() {
        finishReadingIfNeeded()
        super.onStop()
    }

    override fun onDestroyView() {
        finishReadingIfNeeded()
        super.onDestroyView()
        _binding = null
    }
}
