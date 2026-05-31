package com.ui.local

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.R
import com.databinding.DialogLocalCityPickerBinding
import com.databinding.FragmentLocalBinding
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.ui.adapter.NewsAdapter
import com.ui.model.NewsUiModel
import com.util.toDetailBundle
import com.util.gone
import com.util.visible
import com.viewmodel.local.LocalViewModel
import com.viewmodel.local.LocalViewModelFactory
import kotlinx.coroutines.launch

class LocalFragment : Fragment(R.layout.fragment_local) {


    private var _binding: FragmentLocalBinding? = null
    private val binding get() = _binding!!

    private val viewModel: LocalViewModel by viewModels {
        val app = requireActivity().application as com.NewsApp
        LocalViewModelFactory(
            repository = app.repository,
            statsStore = app.localCityStatsStore
        )
    }

    private lateinit var newsAdapter: NewsAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentLocalBinding.bind(view)

        setupRecyclerView()
        setupActions()
        observeUi()
        setupErrorRetry()
        setupEmptyAction()
        setupRefresh()

        if (savedInstanceState == null) {
            viewModel.loadInitialNews()
        }
    }

    private fun setupActions() {
        binding.btnChooseMoreCities.setOnClickListener {
            showChooseMoreCitiesDialog()
        }
    }

    private fun setupRecyclerView() {
        newsAdapter = NewsAdapter(
            onItemClick = ::navigateToDetail,
            onBookmarkClick = ::toggleBookmark
        )
        binding.recyclerLocal.apply {
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

    private fun renderLocationChips(locations: List<String>, selectedTitle: String) {
        binding.sourceChipContainer.removeAllViews()
        locations.forEach { locationTitle ->
            binding.sourceChipContainer.addView(
                createSourceChip(locationTitle, locationTitle == selectedTitle) {
                    viewModel.selectLocation(locationTitle)
                }
            )
        }
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
            LinearLayout.LayoutParams.WRAP_CONTENT
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
        button.strokeColor = ColorStateList.valueOf(
            ContextCompat.getColor(requireContext(), R.color.news_chip_stroke)
        )
        button.cornerRadius = resources.getDimensionPixelSize(R.dimen.chip_corner_radius)
        button.setPaddingRelative(24, 8, 24, 8)
        button.setOnClickListener { onClick() }
        return button
    }

    private fun observeUi() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.locationOptions.collect { options ->
                        renderLocationChips(options.map { it.title }, viewModel.selectedTitle.value)
                    }
                }
                launch {
                    viewModel.selectedTitle.collect { selectedTitle ->
                        renderLocationChips(viewModel.locationOptions.value.map { it.title }, selectedTitle)
                    }
                }
                launch {
                    viewModel.uiState.collect { state ->
                        renderState(state)
                    }
                }
            }
        }
    }

    private fun setupErrorRetry() {
        binding.layoutError.btnRetry.setOnClickListener {
            viewModel.refreshNews()
        }
        binding.layoutError.btnSaved.setOnClickListener {
            findNavController().navigate(R.id.bookmarkFragment)
        }
    }

    private fun setupEmptyAction() {
        binding.layoutEmpty.btnTryOtherCity.setOnClickListener {
            showChooseMoreCitiesDialog()
        }
    }

    private fun setupRefresh() {
        binding.swipeRefreshLocal.setOnRefreshListener {
            viewModel.refreshNews()
        }
    }

    private fun showChooseMoreCitiesDialog() {
        val dialogBinding = DialogLocalCityPickerBinding.inflate(layoutInflater)
        val selectedTitles = viewModel.getSelectedAdditionalCityTitles().toMutableSet()
        val optionAdapter = LocalCityOptionAdapter(
            selectedTitles = selectedTitles,
            onToggle = { title, isChecked ->
                if (isChecked) {
                    selectedTitles += title
                } else {
                    selectedTitles -= title
                }
            },
            hitCountProvider = { cityTitle -> viewModel.getCityHitCount(cityTitle) }
        )

        dialogBinding.recyclerCityOptions.apply {
            adapter = optionAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }

        fun renderOptions(keyword: String) {
            val options = viewModel.searchAdditionalCityOptions(keyword)
            optionAdapter.submitList(options)
            dialogBinding.tvNoCityMatch.isVisible = options.isEmpty()
            dialogBinding.recyclerCityOptions.isVisible = options.isNotEmpty()
        }

        renderOptions("")
        dialogBinding.etCitySearch.doAfterTextChanged { editable ->
            renderOptions(editable?.toString().orEmpty())
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.local_choose_more_cities_title)
            .setView(dialogBinding.root)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.local_apply) { _, _ ->
                viewModel.updateAdditionalCities(selectedTitles)
            }
            .show()
    }

    private fun renderState(state: LocalUiState) {
        binding.layoutLoading.gone()
        binding.recyclerLocal.gone()
        binding.layoutEmpty.root.gone()
        binding.layoutError.root.gone()
        binding.tvOfflineBadge.gone()
        binding.swipeRefreshLocal.isRefreshing = false

        when (state) {
            is LocalUiState.Loading -> binding.layoutLoading.visible()
            is LocalUiState.Refreshing -> {
                newsAdapter.submitList(state.articles)
                binding.recyclerLocal.visible()
                binding.tvOfflineBadge.isVisible = state.isOffline
                binding.swipeRefreshLocal.isRefreshing = true
            }
            is LocalUiState.Success -> {
                newsAdapter.submitList(state.articles)
                binding.recyclerLocal.visible()
                binding.tvOfflineBadge.isVisible = state.isOffline
            }
            is LocalUiState.Empty -> binding.layoutEmpty.root.visible()
            is LocalUiState.Error -> {
                binding.layoutError.root.visible()
                binding.layoutError.tvErrorMessage.text = state.message
            }
        }
    }

    private fun toggleBookmark(item: NewsUiModel) {
        viewModel.toggleBookmark(item)
    }



    private fun navigateToDetail(item: NewsUiModel) {
        if (findNavController().currentDestination?.id != R.id.localFragment) return
        findNavController().navigate(
            R.id.action_localFragment_to_detailFragment,
            item.toDetailBundle("local")
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

