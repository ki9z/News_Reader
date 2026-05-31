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
import androidx.recyclerview.widget.LinearLayoutManager
import com.R
import com.databinding.FragmentFollowingTopicsBinding
import com.ui.adapter.FollowTopicAdapter
import com.util.UiState
import com.util.gone
import com.util.visible
import com.viewmodel.profile.FollowingTopicsViewModel
import com.viewmodel.profile.FollowingTopicsViewModelFactory
import kotlinx.coroutines.launch

class FollowingTopicsFragment : Fragment(R.layout.fragment_following_topics) {

    private var _binding: FragmentFollowingTopicsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: FollowingTopicsViewModel by viewModels {
        val app = requireActivity().application as com.NewsApp
        FollowingTopicsViewModelFactory(app.profileRepository, app.repository, app.followPreferenceRepository)
    }

    private lateinit var adapter: FollowTopicAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentFollowingTopicsBinding.bind(view)

        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }
        setupControls()
        setupRecyclerView()
        observeUi()
    }

    private fun setupControls() {
        binding.etSearch.doAfterTextChanged { editable ->
            viewModel.setQuery(editable?.toString().orEmpty())
        }
        binding.btnResetRecommendations.setOnClickListener {
            viewModel.resetRecommendations()
            Toast.makeText(requireContext(), R.string.profile_reset_recommendations, Toast.LENGTH_SHORT).show()
        }
        binding.btnWhySeeingThis.setOnClickListener {
            Toast.makeText(requireContext(), R.string.profile_why_am_i_seeing_this, Toast.LENGTH_LONG).show()
        }

        binding.chipGroupFollowTab.setOnCheckedStateChangeListener { _, checkedIds ->
            when (checkedIds.firstOrNull()) {
                R.id.chipTabSources -> viewModel.setTab(FollowingTopicsViewModel.FollowTab.SOURCES)
                R.id.chipTabKeywords -> viewModel.setTab(FollowingTopicsViewModel.FollowTab.KEYWORDS)
                else -> viewModel.setTab(FollowingTopicsViewModel.FollowTab.TOPICS)
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = FollowTopicAdapter(
            onToggle = viewModel::toggleTopic,
            onMute = viewModel::toggleMute,
            onBlock = viewModel::blockTopic
        )
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter
    }

    private fun observeUi() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.progressBar.gone()
                    binding.recyclerView.gone()

                    when (state) {
                        is UiState.Loading -> binding.progressBar.visible()
                        is UiState.Success -> {
                            binding.recyclerView.visible()
                            adapter.submitList(state.data)
                        }
                        is UiState.Empty,
                        is UiState.Error,
                        is UiState.Idle -> Unit
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

