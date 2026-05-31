package com.ui.reader

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.os.Build
import android.os.Bundle
import android.view.View
import android.webkit.CookieManager
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.NewsApp
import com.R
import com.data.model.Article
import com.data.model.Source
import com.databinding.FragmentArticleWebviewBinding
import com.util.ArticleContentFormatter
import com.util.ReaderContentExtractor
import com.util.gone
import com.util.visible
import com.viewmodel.detail.DetailViewModel
import com.viewmodel.detail.DetailViewModelFactory
import kotlinx.coroutines.launch

class ArticleWebViewFragment : Fragment(R.layout.fragment_article_webview) {

    private var _binding: FragmentArticleWebviewBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DetailViewModel by viewModels {
        val app = requireActivity().application as NewsApp
        DetailViewModelFactory(
            repository = app.repository,
            profileRepository = app.profileRepository,
            userSettingsRepository = app.userSettingsRepository
        )
    }

    private val articleUrl: String by lazy { requireArguments().getString(ARG_ARTICLE_URL).orEmpty() }
    private val articleTitle: String by lazy { requireArguments().getString(ARG_TITLE).orEmpty() }
    private val articleDescription: String by lazy { requireArguments().getString(ARG_DESCRIPTION).orEmpty() }
    private val articleContent: String by lazy { requireArguments().getString(ARG_CONTENT).orEmpty() }
    private val sourceName: String by lazy { requireArguments().getString(ARG_SOURCE_NAME).orEmpty() }
    private val publishedAt: String by lazy { requireArguments().getString(ARG_PUBLISHED_AT).orEmpty() }
    private val imageUrl: String by lazy { requireArguments().getString(ARG_IMAGE_URL).orEmpty() }
    private val startInReaderMode: Boolean by lazy { requireArguments().getBoolean(ARG_START_IN_READER_MODE, false) }

    private var readerMode = false
    private var readerFetchStarted = false
    private var latestReaderText: String = ""
    private var hasRecordedOriginalOpen = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentArticleWebviewBinding.bind(view)

        latestReaderText = bestFallbackContent()
        setupToolbar()
        setupWebView()
        renderReaderContent(latestReaderText, isLoading = false)

        if (startInReaderMode) {
            showReaderMode()
        } else {
            showWebMode()
        }
    }

    private fun setupToolbar() {
        binding.toolbar.title = articleTitle.ifBlank { getString(R.string.read_full_article) }
        binding.toolbar.setNavigationOnClickListener { findNavController().popBackStack() }
        binding.toolbar.inflateMenu(R.menu.menu_article_webview)
        updateReaderMenuTitle()
        binding.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_toggle_reader -> {
                    if (readerMode) showWebMode() else showReaderMode()
                    true
                }
                R.id.action_open_in_browser -> {
                    openInBrowser()
                    true
                }
                else -> false
            }
        }
    }

    @TargetApi(26)
    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        CookieManager.getInstance().setAcceptCookie(false)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WebView.startSafeBrowsing(requireContext()) { }
        }

        binding.webView.apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.loadsImagesAutomatically = true
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            settings.allowFileAccessFromFileURLs = false
            settings.allowUniversalAccessFromFileURLs = false
            settings.cacheMode = WebSettings.LOAD_DEFAULT
            settings.useWideViewPort = true
            settings.loadWithOverviewMode = true
            settings.builtInZoomControls = false
            settings.displayZoomControls = false
            settings.mediaPlaybackRequiresUserGesture = true
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                settings.safeBrowsingEnabled = true
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                CookieManager.getInstance().setAcceptThirdPartyCookies(this, false)
            }

            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    if (readerMode) return
                    binding.progressBar.isIndeterminate = false
                    binding.progressBar.progress = newProgress.coerceIn(0, 100)
                    if (newProgress >= 100) binding.progressBar.gone() else binding.progressBar.visible()
                }
            }

            webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    if (readerMode) return
                    binding.progressBar.isIndeterminate = true
                    binding.progressBar.visible()
                    binding.errorContainer.gone()
                    binding.webView.visible()
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    if (!readerMode) binding.progressBar.gone()
                }

                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                    val url = request?.url?.toString().orEmpty()
                    if (!isSafeHttpUrl(url)) {
                        runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
                        return true
                    }
                    return false
                }

                override fun onReceivedError(
                    view: WebView?,
                    request: WebResourceRequest?,
                    error: WebResourceError?
                ) {
                    if (!readerMode && request?.isForMainFrame == true) {
                        showError()
                    }
                }

                override fun onReceivedSslError(
                    view: WebView?,
                    handler: SslErrorHandler?,
                    error: SslError?
                ) {
                    handler?.cancel()
                    if (!readerMode) showError()
                }
            }
        }

        binding.btnRetry.setOnClickListener {
            if (readerMode) {
                fetchReaderContent(force = true)
            } else {
                loadArticle()
            }
        }
        binding.btnOpenBrowser.setOnClickListener { openInBrowser() }
        binding.btnReaderOpenBrowser.setOnClickListener { openInBrowser() }
    }

    private fun showWebMode() {
        readerMode = false
        updateReaderMenuTitle()
        binding.readerScroll.gone()
        binding.errorContainer.gone()
        binding.webView.visible()
        loadArticle()
    }

    private fun showReaderMode() {
        readerMode = true
        updateReaderMenuTitle()
        binding.webView.gone()
        binding.errorContainer.gone()
        binding.readerScroll.visible()
        binding.progressBar.gone()
        renderReaderContent(latestReaderText, isLoading = latestReaderText.isBlank())
        fetchReaderContent(force = false)
    }

    private fun updateReaderMenuTitle() {
        binding.toolbar.menu.findItem(R.id.action_toggle_reader)?.title = getString(
            if (readerMode) R.string.article_view_original else R.string.article_reader_mode
        )
    }

    private fun fetchReaderContent(force: Boolean) {
        if (readerFetchStarted && !force) return
        if (!isSafeHttpUrl(articleUrl)) return
        readerFetchStarted = true
        renderReaderContent(latestReaderText, isLoading = true)

        viewLifecycleOwner.lifecycleScope.launch {
            val extracted = ReaderContentExtractor.fetchReadableArticle(
                url = articleUrl,
                fallbackTitle = articleTitle
            )
            val fetchedText = extracted?.text.orEmpty()
            latestReaderText = listOf(fetchedText, latestReaderText)
                .maxByOrNull { it.length }
                .orEmpty()
            renderReaderContent(latestReaderText, isLoading = false)
        }
    }

    private fun renderReaderContent(text: String, isLoading: Boolean) {
        binding.tvReaderTitle.text = articleTitle.ifBlank { getString(R.string.read_full_article) }
        binding.tvReaderMeta.text = listOf(sourceName, publishedAt)
            .filter { it.isNotBlank() }
            .joinToString(" • ")
            .ifBlank { getString(R.string.unknown_source) }

        val displayText = text.ifBlank {
            if (isLoading) getString(R.string.article_reader_loading)
            else getString(R.string.article_reader_unavailable)
        }
        binding.tvReaderContent.text = displayText
        binding.readerProgress.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    private fun bestFallbackContent(): String {
        val sanitizedDescription = ArticleContentFormatter.sanitize(articleDescription)
        val sanitizedContent = ArticleContentFormatter.sanitize(articleContent)
        val deduplicatedContent = ArticleContentFormatter.removeDuplicateSummary(
            description = sanitizedDescription,
            content = sanitizedContent
        )
        return listOf(deduplicatedContent, sanitizedDescription)
            .firstOrNull { it.isNotBlank() }
            .orEmpty()
    }

    private fun loadArticle() {
        val safeUrl = articleUrl.trim()
        if (!isSafeHttpUrl(safeUrl)) {
            showError()
            return
        }

        recordOriginalOpenIfNeeded()
        binding.errorContainer.gone()
        binding.webView.visible()
        binding.webView.loadUrl(safeUrl)
    }

    private fun showError() {
        binding.progressBar.gone()
        binding.webView.gone()
        binding.readerScroll.gone()
        binding.errorContainer.visible()
    }

    private fun openInBrowser() {
        val safeUrl = articleUrl.trim()
        if (safeUrl.isBlank()) {
            Toast.makeText(requireContext(), R.string.profile_download_invalid, Toast.LENGTH_SHORT).show()
            return
        }
        recordOriginalOpenIfNeeded()
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(safeUrl)))
    }

    private fun recordOriginalOpenIfNeeded() {
        if (hasRecordedOriginalOpen) return

        val safeUrl = articleUrl.trim()
        if (safeUrl.isBlank()) return

        hasRecordedOriginalOpen = true
        viewModel.recordOriginalArticleOpen(
            Article(
                source = Source(id = null, name = sourceName.ifBlank { null }),
                author = null,
                title = articleTitle.ifBlank { null },
                description = articleDescription.ifBlank { null },
                url = safeUrl,
                urlToImage = imageUrl.ifBlank { null },
                publishedAt = publishedAt.ifBlank { null },
                content = articleContent.ifBlank { null }
            )
        )
    }

    private fun isSafeHttpUrl(url: String): Boolean {
        return url.startsWith("https://", ignoreCase = true)
    }

    override fun onDestroyView() {
        binding.webView.apply {
            stopLoading()
            webChromeClient = null
            removeAllViews()
            destroy()
        }
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val ARG_TITLE = "title"
        const val ARG_ARTICLE_URL = "articleUrl"
        const val ARG_DESCRIPTION = "description"
        const val ARG_CONTENT = "content"
        const val ARG_SOURCE_NAME = "sourceName"
        const val ARG_PUBLISHED_AT = "publishedAt"
        const val ARG_IMAGE_URL = "imageUrl"
        const val ARG_START_IN_READER_MODE = "startInReaderMode"
    }
}
