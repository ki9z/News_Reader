package com.ui.home

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.R
import com.databinding.FragmentHomeBinding
import com.databinding.SheetHomeFilterBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.ui.adapter.HomeNewsAdapter
import com.ui.model.NewsUiModel
import com.util.toDetailBundle
import com.util.Constants
import com.util.UiState
import com.util.gone
import com.util.visible
import com.viewmodel.home.HomeViewModel
import com.viewmodel.home.HomeViewModelFactory
import kotlinx.coroutines.launch

class HomeFragment : Fragment(R.layout.fragment_home) {

    private data class HomeTab(
        val titleRes: Int,
        val colorRes: Int,
        val tab: HomeViewModel.HomeFeedTab
    )

    private data class FilterOption(
        val label: String,
        val value: String
    )

    private data class SourceOption(
        val label: String,
        val sourceId: String?
    )

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels {
        val app = requireActivity().application as com.NewsApp
        HomeViewModelFactory(app.repository, app.profileRepository, app.followPreferenceRepository)
    }

    private lateinit var newsAdapter: HomeNewsAdapter

    private val tabs: List<HomeTab> = listOf(
        HomeTab(R.string.home_tab_featured, R.color.news_tab_top, HomeViewModel.HomeFeedTab.FEATURED),
        HomeTab(R.string.home_tab_local, R.color.news_tab_local, HomeViewModel.HomeFeedTab.LOCAL),
        HomeTab(R.string.home_tab_following, R.color.news_tab_following, HomeViewModel.HomeFeedTab.FOLLOWING)
    )

    private val countryOptions = listOf(
        FilterOption("United States", "us"),
        FilterOption("Vietnam", "vn"),
        FilterOption("United Kingdom", "gb"),
        FilterOption("Japan", "jp"),
        FilterOption("Korea", "kr"),
        FilterOption("France", "fr"),
        FilterOption("Germany", "de"),
        FilterOption("Australia", "au")
    )

    private val categoryOptions = listOf(
        FilterOption("General", Constants.CATEGORY_GENERAL),
        FilterOption("Business", Constants.CATEGORY_BUSINESS),
        FilterOption("Technology", Constants.CATEGORY_TECHNOLOGY),
        FilterOption("Entertainment", Constants.CATEGORY_ENTERTAINMENT),
        FilterOption("Sports", Constants.CATEGORY_SPORTS),
        FilterOption("Science", "science"),
        FilterOption("Health", Constants.CATEGORY_HEALTH)
    )

    private val sortOptions = listOf(
        FilterOption("Newest", "publishedAt"),
        FilterOption("Most Popular", "popularity"),
        FilterOption("Relevant", "relevancy")
    )

    private val manualSourceOptions = listOf(
        SourceOption("All", null),
        SourceOption("CNN", "cnn"),
        SourceOption("BBC", "bbc-news"),
        SourceOption("Reuters", "reuters"),
        SourceOption("Associated Press", "associated-press"),
        SourceOption("TechCrunch", "techcrunch")
    )

    private var selectedTab: HomeTab = tabs.first()
    private var currentItems: List<NewsUiModel> = emptyList()
    private val hiddenSources = linkedSetOf<String>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentHomeBinding.bind(view)

        setupRecyclerView()
        setupHeaderActions()
        setupRetryAndEmptyActions()
        setupRefresh()
        renderCategoryTabs()
        renderContentCategoryChips()
        observeUi()

        if (savedInstanceState == null) {
            viewModel.loadInitialNews()
        }
    }

    private fun setupHeaderActions() {
        binding.btnRefresh.setOnClickListener { viewModel.refreshNews() }
        binding.btnSettings.setOnClickListener { showFilterBottomSheet() }
        binding.searchInputLayout.setOnClickListener { findNavController().navigate(R.id.searchFragment) }
        binding.etHomeSearch.setOnClickListener { findNavController().navigate(R.id.searchFragment) }
    }

    private fun setupRetryAndEmptyActions() {
        binding.layoutError.btnRetry.setOnClickListener { viewModel.refreshNews() }
        binding.layoutError.btnSaved.setOnClickListener {
            findNavController().navigate(R.id.bookmarkFragment)
        }
        binding.layoutEmpty.btnTryOtherCity.setOnClickListener {
            viewModel.refreshNews()
        }
    }

    private fun setupRefresh() {
        binding.swipeRefreshHome.setOnRefreshListener {
            viewModel.refreshNews()
        }
    }

    private fun setupRecyclerView() {
        newsAdapter = HomeNewsAdapter(
            onItemClick = ::navigateToDetail,
            onBookmarkClick = ::toggleBookmark,
            onItemLongClick = ::showItemActions
        )

        binding.recyclerHome.apply {
            adapter = newsAdapter
            layoutManager = LinearLayoutManager(requireContext())
            addOnScrollListener(object : androidx.recyclerview.widget.RecyclerView.OnScrollListener() {
                override fun onScrolled(
                    recyclerView: androidx.recyclerview.widget.RecyclerView,
                    dx: Int,
                    dy: Int
                ) {
                    super.onScrolled(recyclerView, dx, dy)
                    if (dy <= 0) return
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
    }

    private fun renderCategoryTabs() {
        binding.categoryTabContainer.removeAllViews()
        tabs.forEach { tab ->
            binding.categoryTabContainer.addView(
                createCategoryButton(tab, tab == selectedTab) {
                    if (selectedTab == tab) return@createCategoryButton
                    selectedTab = tab
                    hiddenSources.clear()
                    renderCategoryTabs()
                    renderContentCategoryChips()
                    viewModel.selectTab(tab.tab)
                }
            )
        }
    }

    private fun renderContentCategoryChips() {
        val currentFilters = viewModel.currentFilterState()

        binding.sourceChipContainer.removeAllViews()
        categoryOptions.forEach { category ->
            val selected = category.value == currentFilters.category
            binding.sourceChipContainer.addView(
                createSourceChip(category.label, selected) {
                    if (selected) return@createSourceChip

                    hiddenSources.clear()
                    viewModel.applyFilters(
                        currentFilters.copy(
                            category = category.value,
                            sourceId = null
                        )
                    )
                    renderContentCategoryChips()
                }
            )
        }
    }

    private fun createCategoryButton(
        tab: HomeTab,
        selected: Boolean,
        onClick: () -> Unit
    ): MaterialButton {
        val button = MaterialButton(
            requireContext(),
            null,
            com.google.android.material.R.attr.materialButtonOutlinedStyle
        )
        button.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            resources.getDimensionPixelSize(R.dimen.home_tab_height)
        ).apply {
            marginEnd = resources.getDimensionPixelSize(R.dimen.category_button_spacing)
        }

        val selectedText = ContextCompat.getColor(requireContext(), R.color.news_bg_white)
        val normalBg = ContextCompat.getColor(requireContext(), R.color.news_tab_unselected_bg)
        val normalText = ContextCompat.getColor(requireContext(), R.color.news_secondary_text)
        val accentBg = ContextCompat.getColor(requireContext(), tab.colorRes)

        button.text = getString(tab.titleRes)
        button.backgroundTintList = ColorStateList.valueOf(if (selected) accentBg else normalBg)
        button.setTextColor(if (selected) selectedText else normalText)
        button.strokeWidth = 0
        button.cornerRadius = resources.getDimensionPixelSize(R.dimen.tab_corner_radius)
        button.insetTop = 0
        button.insetBottom = 0
        button.setOnClickListener { onClick() }
        return button
    }

    private fun createSourceChip(
        title: String,
        selected: Boolean,
        onClick: () -> Unit
    ): MaterialButton {
        val button = MaterialButton(
            requireContext(),
            null,
            com.google.android.material.R.attr.materialButtonOutlinedStyle
        )
        button.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            resources.getDimensionPixelSize(R.dimen.home_chip_height)
        ).apply {
            marginEnd = resources.getDimensionPixelSize(R.dimen.category_button_spacing)
        }

        val selectedBg = ContextCompat.getColor(requireContext(), R.color.news_chip_selected_bg)
        val selectedText = ContextCompat.getColor(requireContext(), R.color.news_bg_white)
        val normalBg = ContextCompat.getColor(requireContext(), R.color.news_bg_white)
        val normalText = ContextCompat.getColor(requireContext(), R.color.news_primary_text)

        button.text = title
        button.backgroundTintList = ColorStateList.valueOf(if (selected) selectedBg else normalBg)
        button.setTextColor(if (selected) selectedText else normalText)
        button.strokeWidth = resources.getDimensionPixelSize(R.dimen.chip_stroke_width)
        button.strokeColor = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.news_chip_stroke))
        button.cornerRadius = resources.getDimensionPixelSize(R.dimen.chip_corner_radius)
        button.insetTop = 0
        button.insetBottom = 0
        button.setOnClickListener { onClick() }
        return button
    }

    private fun observeUi() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { state ->
                        binding.btnRefresh.isEnabled = state !is UiState.Loading
                        renderState(state)
                    }
                }

                launch {
                    viewModel.refreshNotice.collect { notice ->
                        when (notice) {
                            HomeViewModel.RefreshNotice.Updated -> {
                                toast(getString(R.string.home_refresh_updated))
                            }
                            HomeViewModel.RefreshNotice.NoNew -> {
                                toast(getString(R.string.home_refresh_no_new))
                            }
                            HomeViewModel.RefreshNotice.CoolingDown -> {
                                toast(getString(R.string.home_refresh_cooldown))
                            }
                            HomeViewModel.RefreshNotice.Debounced -> {
                                toast(getString(R.string.home_refresh_debounce))
                            }
                            HomeViewModel.RefreshNotice.None -> Unit
                        }
                    }
                }

                launch {
                    viewModel.loadMoreError.collect { message ->
                        if (message.isNullOrBlank()) return@collect

                        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
                        viewModel.clearLoadMoreError()
                    }
                }
            }
        }
    }

    private fun renderState(state: UiState<List<NewsUiModel>>) {
        binding.layoutLoading.gone()
        binding.recyclerHome.gone()
        binding.layoutEmpty.root.gone()
        binding.layoutError.root.gone()
        binding.swipeRefreshHome.isRefreshing = false

        when (state) {
            is UiState.Success -> {
                currentItems = state.data
                renderContentCategoryChips()
                applyVisibleArticleFilters()
                binding.recyclerHome.visible()
            }
            is UiState.Empty -> {
                currentItems = emptyList()
                renderContentCategoryChips()
                newsAdapter.submitList(emptyList())
                binding.layoutEmpty.root.visible()
            }
            is UiState.Error -> {
                binding.layoutError.root.visible()
                binding.layoutError.tvErrorMessage.text = state.message
            }
            is UiState.Loading -> {
                if (currentItems.isNotEmpty()) {
                    binding.recyclerHome.visible()
                    binding.swipeRefreshHome.isRefreshing = true
                } else {
                    binding.layoutLoading.visible()
                }
            }
            is UiState.Idle -> Unit
        }
    }

    private fun applyVisibleArticleFilters() {
        val visibleItems = currentItems.filterNot {
            hiddenSources.contains(it.sourceName.trim().lowercase())
        }
        newsAdapter.submitList(visibleItems)
    }

    private fun toggleBookmark(item: NewsUiModel) {
        viewModel.toggleBookmark(item)
        toast(
            getString(
                if (item.isBookmarked) R.string.home_bookmark_removed else R.string.home_bookmark_added
            )
        )
    }

    private fun showItemActions(item: NewsUiModel) {
        val popup = PopupMenu(requireContext(), binding.recyclerHome)
        popup.menu.add(0, 1, 0, getString(R.string.home_action_save))
        popup.menu.add(0, 2, 1, getString(R.string.home_action_share))
        popup.menu.add(0, 3, 2, getString(R.string.home_action_hide_source))

        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                1 -> {
                    toggleBookmark(item)
                    true
                }
                2 -> {
                    shareArticle(item)
                    true
                }
                3 -> {
                    hideSource(item.sourceName)
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    private fun shareArticle(item: NewsUiModel) {
        val text = listOfNotNull(item.title, item.articleUrl).joinToString("\n")
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        startActivity(Intent.createChooser(shareIntent, getString(R.string.home_action_share)))
    }

    private fun hideSource(sourceName: String) {
        val normalized = sourceName.trim().lowercase()
        if (normalized.isBlank()) return
        hiddenSources += normalized
        applyVisibleArticleFilters()
        toast(getString(R.string.home_hide_source_done))
    }

    private fun showFilterBottomSheet() {
        val dialog = BottomSheetDialog(requireContext())
        val sheetBinding = SheetHomeFilterBinding.inflate(layoutInflater)
        dialog.setContentView(sheetBinding.root)

        val currentFilters = viewModel.currentFilterState()

        setupDropdown(
            sheetBinding.actCountry,
            countryOptions.map { it.label },
            countryOptions.firstOrNull { it.value == currentFilters.countryCode }?.label
        )
        setupDropdown(
            sheetBinding.actCategory,
            categoryOptions.map { it.label },
            categoryOptions.firstOrNull { it.value == currentFilters.category }?.label
        )
        setupDropdown(
            sheetBinding.actSort,
            sortOptions.map { it.label },
            sortOptions.firstOrNull { it.value == currentFilters.sortBy }?.label
        )
        setupDropdown(
            sheetBinding.actSource,
            manualSourceOptions.map { it.label },
            manualSourceOptions.firstOrNull { it.sourceId == currentFilters.sourceId }?.label
        )

        sheetBinding.btnResetFilter.setOnClickListener {
            sheetBinding.actCountry.setText(countryOptions.first().label, false)
            sheetBinding.actCategory.setText(categoryOptions.first().label, false)
            sheetBinding.actSort.setText(sortOptions.first().label, false)
            sheetBinding.actSource.setText(manualSourceOptions.first().label, false)
        }

        sheetBinding.btnApplyFilter.setOnClickListener {
            val country = countryOptions.firstOrNull { it.label == sheetBinding.actCountry.text.toString() }
                ?: countryOptions.first()
            val category = categoryOptions.firstOrNull { it.label == sheetBinding.actCategory.text.toString() }
                ?: categoryOptions.first()
            val sort = sortOptions.firstOrNull { it.label == sheetBinding.actSort.text.toString() }
                ?: sortOptions.first()
            val source = manualSourceOptions.firstOrNull { it.label == sheetBinding.actSource.text.toString() }
                ?: manualSourceOptions.first()

            viewModel.applyFilters(
                HomeViewModel.HomeFilterState(
                    countryCode = country.value,
                    category = category.value,
                    sortBy = sort.value,
                    sourceId = source.sourceId
                )
            )

            hiddenSources.clear()
            renderContentCategoryChips()
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun setupDropdown(
        view: com.google.android.material.textfield.MaterialAutoCompleteTextView,
        options: List<String>,
        selectedValue: String?
    ) {
        view.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, options))
        val defaultValue = selectedValue ?: options.firstOrNull().orEmpty()
        if (defaultValue.isNotBlank()) {
            view.setText(defaultValue, false)
        }
    }

    private fun navigateToDetail(item: NewsUiModel) {
        if (findNavController().currentDestination?.id != R.id.homeFragment) return
        findNavController().navigate(
            R.id.action_homeFragment_to_detailFragment,
            item.toDetailBundle("home")
        )
    }

    private fun toast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
