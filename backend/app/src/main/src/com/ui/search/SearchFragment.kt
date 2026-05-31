package com.ui.search

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.R
import com.databinding.FragmentSearchBinding
import com.google.android.material.chip.Chip
import com.ui.adapter.ExploreTopicAdapter
import com.ui.adapter.TrendingAdapter
import com.ui.model.ExploreTopicItem
import com.ui.model.MockUiData
import com.ui.model.NewsUiModel
import com.util.toDetailBundle
import com.ui.model.TrendingItem
import com.util.UiState
import com.util.gone
import com.util.visible
import com.viewmodel.search.SearchViewModel
import com.viewmodel.search.SearchViewModelFactory
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SearchFragment : Fragment(R.layout.fragment_search) {

    companion object {
        private const val EXPLORE_PREVIEW_COUNT = 5
    }

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!

    private lateinit var exploreAdapter: ExploreTopicAdapter
    private lateinit var trendingAdapter: TrendingAdapter

    private var pendingDetailNavigation = false
    private var searchJob: Job? = null
    private var isExploreExpanded = false
    private var currentQuery: String = ""
    private var currentSearchItems: List<NewsUiModel> = emptyList()

    private val viewModel: SearchViewModel by viewModels {
        val app = requireActivity().application as com.NewsApp
        SearchViewModelFactory(app.repository, app.profileRepository)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentSearchBinding.bind(view)

        setupRecyclerView()
        setupSearch()
        setupStateActions()
        observeSearchResult()
        applyFilter("")
    }

    private fun setupStateActions() {
        binding.layoutError.btnRetry.setOnClickListener {
            retryCurrentRequest()
        }

        binding.layoutError.btnSaved.setOnClickListener {
            findNavController().navigate(R.id.bookmarkFragment)
        }

        binding.layoutEmpty.btnTryOtherCity.apply {
            text = getString(R.string.retry)
            setOnClickListener { retryCurrentRequest() }
        }

        binding.swipeRefreshSearch.setOnRefreshListener {
            retryCurrentRequest()
        }
    }

    private fun retryCurrentRequest() {
        if (currentQuery.isBlank()) {
            viewModel.loadTrendingNews()
        } else {
            viewModel.searchNews(currentQuery)
        }
    }

    private fun setupRecyclerView() {
        exploreAdapter = ExploreTopicAdapter(::openTopicArticle)
        binding.recyclerExploreTopics.apply {
            adapter = exploreAdapter
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        }

        trendingAdapter = TrendingAdapter(::openTrendingArticle)
        binding.recyclerTrending.apply {
            adapter = trendingAdapter
            layoutManager = LinearLayoutManager(requireContext())
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    if (dy <= 0 || currentQuery.isBlank()) return

                    val manager = recyclerView.layoutManager as? LinearLayoutManager ?: return
                    val visibleCount = manager.childCount
                    val totalCount = manager.itemCount
                    val firstVisible = manager.findFirstVisibleItemPosition()

                    if (visibleCount + firstVisible >= totalCount - 4) {
                        viewModel.loadMoreNews()
                    }
                }
            })
        }

        renderExploreTopics()

        binding.tvSeeMoreTopics.setOnClickListener {
            isExploreExpanded = !isExploreExpanded
            renderExploreTopics()
        }
    }

    private fun renderExploreTopics() {
        val topics = if (isExploreExpanded) {
            MockUiData.exploreTopics
        } else {
            MockUiData.exploreTopics.take(EXPLORE_PREVIEW_COUNT)
        }

        exploreAdapter.submitList(topics)

        binding.tvSeeMoreTopics.visibility =
            if (MockUiData.exploreTopics.size > EXPLORE_PREVIEW_COUNT) View.VISIBLE else View.GONE

        binding.tvSeeMoreTopics.text = getString(
            if (isExploreExpanded) R.string.search_see_less_topics else R.string.search_see_more_topics
        )
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit

            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString().orEmpty()
                searchJob?.cancel()
                searchJob = viewLifecycleOwner.lifecycleScope.launch {
                    delay(450)
                    applyFilter(query)
                }
            }
        })
    }

    private fun applyFilter(query: String) {
        val normalizedQuery = query.trim()
        currentQuery = normalizedQuery
        pendingDetailNavigation = false

        if (normalizedQuery.isBlank()) {
            viewModel.clearSearch()
            viewModel.loadTrendingNews()
            renderRecentSearches(viewModel.recentSearchQueries.value)
            return
        }

        if (normalizedQuery.length < 2) {
            viewModel.clearSearch()
            binding.groupRecentSearches.gone()
            currentSearchItems = emptyList()
            trendingAdapter.submitList(emptyList())
            renderContentState()
            return
        }

        binding.groupRecentSearches.gone()
        viewModel.searchNews(normalizedQuery)
    }

    private fun observeSearchResult() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { state ->
                        if (currentQuery.isBlank()) return@collect

                        when (state) {
                            is UiState.Success -> {
                                currentSearchItems = state.data
                                renderTrendingFromSearch(state.data)
                                renderContentState()

                                if (pendingDetailNavigation) {
                                    pendingDetailNavigation = false
                                    state.data.firstOrNull()?.let(::navigateToDetail)
                                        ?: Toast.makeText(
                                            requireContext(),
                                            getString(R.string.no_trending_result),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                }
                            }

                            is UiState.Empty -> {
                                currentSearchItems = emptyList()
                                trendingAdapter.submitList(emptyList())
                                renderEmptyState()

                                if (pendingDetailNavigation) {
                                    pendingDetailNavigation = false
                                    Toast.makeText(
                                        requireContext(),
                                        getString(R.string.no_trending_result),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }

                            is UiState.Error -> {
                                if (pendingDetailNavigation) {
                                    pendingDetailNavigation = false
                                }

                                currentSearchItems = emptyList()
                                trendingAdapter.submitList(emptyList())
                                renderErrorState(state.message)
                            }

                            is UiState.Loading -> {
                                renderLoadingState()
                            }

                            is UiState.Idle -> {
                                if (currentQuery.isNotBlank()) {
                                    renderContentState()
                                }
                            }
                        }
                    }
                }

                launch {
                    viewModel.trendingState.collect { state ->
                        if (currentQuery.isNotBlank()) return@collect

                        when (state) {
                            is UiState.Success -> {
                                currentSearchItems = state.data
                                renderTrendingFromSearch(state.data)
                                renderContentState()
                            }

                            is UiState.Empty -> {
                                currentSearchItems = emptyList()
                                trendingAdapter.submitList(emptyList())
                                renderEmptyState()
                            }

                            is UiState.Error -> {
                                currentSearchItems = emptyList()
                                trendingAdapter.submitList(emptyList())
                                renderErrorState(state.message)
                            }

                            is UiState.Loading -> {
                                renderLoadingState()
                            }

                            is UiState.Idle -> {
                                renderContentState()
                            }
                        }
                    }
                }

                launch {
                    viewModel.recentSearchQueries.collect { queries ->
                        renderRecentSearches(queries)
                    }
                }
            }
        }
    }

    private fun renderRecentSearches(queries: List<String>) {
        if (_binding == null) return

        if (queries.isEmpty() || currentQuery.isNotBlank()) {
            binding.groupRecentSearches.gone()
            binding.chipGroupRecentSearches.removeAllViews()
            return
        }

        binding.groupRecentSearches.visible()
        binding.chipGroupRecentSearches.removeAllViews()

        queries.forEach { query ->
            val chip = Chip(requireContext()).apply {
                text = query
                isCheckable = false
                isClickable = true
                setOnClickListener {
                    binding.etSearch.setText(query)
                    binding.etSearch.setSelection(query.length)
                }
            }

            binding.chipGroupRecentSearches.addView(chip)
        }
    }

    private fun openTrendingArticle(item: TrendingItem) {
        val selectedItem = currentSearchItems.getOrNull(item.rank - 1)

        if (selectedItem != null) {
            navigateToDetail(selectedItem)
            return
        }

        openArticleFromQuery(item.title)
    }

    private fun openTopicArticle(item: ExploreTopicItem) {
        openArticleFromQuery(item.title)
    }

    private fun openArticleFromQuery(query: String) {
        val normalizedQuery = query.trim()
        if (normalizedQuery.isBlank()) return

        currentQuery = normalizedQuery
        pendingDetailNavigation = true
        searchJob?.cancel()
        binding.etSearch.setText(normalizedQuery)
        binding.etSearch.setSelection(normalizedQuery.length)
        searchJob?.cancel()
        viewModel.searchNews(normalizedQuery)
    }

    private fun renderTrendingFromSearch(items: List<NewsUiModel>) {
        val mappedItems = items.mapIndexed { index, item ->
            TrendingItem(
                rank = index + 1,
                title = item.title,
                description = item.description.orEmpty().ifBlank { item.sourceName },
                thumbnailUrl = item.imageUrl.orEmpty()
            )
        }

        trendingAdapter.submitList(mappedItems)
        binding.tvNoResult.visibility = if (mappedItems.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun renderLoadingState() {
        binding.layoutLoading.visible()
        binding.searchScrollContent.gone()
        binding.layoutEmpty.root.gone()
        binding.layoutError.root.gone()
        binding.swipeRefreshSearch.isRefreshing = currentSearchItems.isNotEmpty()

        if (currentSearchItems.isNotEmpty()) {
            binding.layoutLoading.gone()
            binding.searchScrollContent.visible()
        }
    }

    private fun renderContentState() {
        binding.layoutLoading.gone()
        binding.layoutEmpty.root.gone()
        binding.layoutError.root.gone()
        binding.searchScrollContent.visible()
        binding.swipeRefreshSearch.isRefreshing = false
    }

    private fun renderEmptyState() {
        binding.layoutLoading.gone()
        binding.searchScrollContent.gone()
        binding.layoutError.root.gone()
        binding.layoutEmpty.root.visible()
        binding.swipeRefreshSearch.isRefreshing = false
    }

    private fun renderErrorState(message: String) {
        binding.layoutLoading.gone()
        binding.searchScrollContent.gone()
        binding.layoutEmpty.root.gone()
        binding.layoutError.root.visible()
        binding.layoutError.tvErrorMessage.text = message
        binding.swipeRefreshSearch.isRefreshing = false
    }

    private fun navigateToDetail(item: NewsUiModel) {
        val safeUrl = item.articleUrl.orEmpty()
        if (safeUrl.isBlank()) return
        if (findNavController().currentDestination?.id != R.id.searchFragment) return

        findNavController().navigate(
            R.id.action_searchFragment_to_detailFragment,
            item.toDetailBundle("search")
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        pendingDetailNavigation = false
        searchJob?.cancel()
        searchJob = null
        _binding = null
    }
}