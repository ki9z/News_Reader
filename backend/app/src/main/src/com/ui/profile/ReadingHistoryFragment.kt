package com.ui.profile

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.R
import com.databinding.FragmentReadingHistoryBinding
import com.ui.adapter.NewsAdapter
import com.ui.model.NewsUiModel
import com.util.toDetailBundle
import com.util.UiState
import com.util.gone
import com.util.visible
import com.viewmodel.profile.ProfileViewModel
import com.viewmodel.profile.ProfileViewModelFactory
import com.viewmodel.profile.ReadingHistoryViewModel
import com.viewmodel.profile.ReadingHistoryViewModelFactory
import kotlinx.coroutines.launch

class ReadingHistoryFragment : Fragment(R.layout.fragment_reading_history) {

    private var _binding: FragmentReadingHistoryBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ReadingHistoryViewModel by viewModels {
        val app = requireActivity().application as com.NewsApp
        ReadingHistoryViewModelFactory(app.profileRepository)
    }

    private val profileViewModel: ProfileViewModel by viewModels {
        val app = requireActivity().application as com.NewsApp
        ProfileViewModelFactory(
            app.userSettingsRepository,
            app.appDatabase.userDao(),
            app.tokenManager
        )
    }

    private lateinit var adapter: NewsAdapter
    private lateinit var continueAdapter: NewsAdapter
    private var bindingFromState = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentReadingHistoryBinding.bind(view)

        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }
        setupControls()
        setupRecyclerView()
        observeUi()
    }

    private fun setupControls() {
        binding.etSearch.doAfterTextChanged { editable ->
            viewModel.setQuery(editable?.toString().orEmpty())
        }

        binding.chipGroupRange.setOnCheckedStateChangeListener { _, checkedIds ->
            when (checkedIds.firstOrNull()) {
                R.id.chipToday -> viewModel.setRange(ReadingHistoryViewModel.RangeFilter.TODAY)
                R.id.chip30Days -> viewModel.setRange(ReadingHistoryViewModel.RangeFilter.DAYS_30)
                else -> viewModel.setRange(ReadingHistoryViewModel.RangeFilter.DAYS_7)
            }
        }

        binding.btnClearAll.setOnClickListener {
            viewModel.clearAll()
            Toast.makeText(requireContext(), R.string.profile_clear_all, Toast.LENGTH_SHORT).show()
        }

        binding.switchTrackHistory.setOnCheckedChangeListener { _, isChecked ->
            if (bindingFromState) return@setOnCheckedChangeListener
            profileViewModel.setTrackReadingHistory(isChecked)
        }
    }

    private fun setupRecyclerView() {
        adapter = NewsAdapter(::openDetail)
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        continueAdapter = NewsAdapter(::openDetail)
        binding.recyclerContinue.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerContinue.adapter = continueAdapter

        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val item = adapter.currentList.getOrNull(viewHolder.adapterPosition) ?: return
                viewModel.removeItem(item)
                Toast.makeText(requireContext(), R.string.profile_remove_from_history, Toast.LENGTH_SHORT).show()
            }
        }).attachToRecyclerView(binding.recyclerView)
    }

    private fun observeUi() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.progressBar.gone()
                    binding.recyclerView.gone()
                    binding.layoutEmpty.root.gone()

                    when (state) {
                        is UiState.Loading -> binding.progressBar.visible()

                        is UiState.Success -> {
                            binding.recyclerView.visible()
                            adapter.submitList(state.data)
                        }

                        is UiState.Empty -> binding.layoutEmpty.root.visible()

                        is UiState.Error,
                        is UiState.Idle -> Unit
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.continueReading.collect { items ->
                    if (items.isEmpty()) {
                        binding.tvContinueTitle.gone()
                        binding.recyclerContinue.gone()
                    } else {
                        binding.tvContinueTitle.visible()
                        binding.recyclerContinue.visible()
                    }

                    continueAdapter.submitList(items)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                profileViewModel.settings.collect { settings ->
                    bindingFromState = true
                    binding.switchTrackHistory.isChecked = settings.trackReadingHistory
                    bindingFromState = false
                }
            }
        }
    }

    private fun openDetail(item: NewsUiModel) {
        val articleUrl = item.articleUrl.orEmpty()
        if (articleUrl.isBlank()) return

        findNavController().navigate(
            R.id.action_readingHistoryFragment_to_detailFragment,
            item.toDetailBundle("reading_history")
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}