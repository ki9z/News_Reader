package com.ui.bookmark

import android.os.Bundle
import android.view.View
import androidx.appcompat.content.res.AppCompatResources
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
import com.databinding.FragmentBookmarkBinding
import com.google.android.material.snackbar.Snackbar
import com.ui.adapter.NewsAdapter
import com.ui.model.NewsUiModel
import com.util.toDetailBundle
import com.util.UiState
import com.util.gone
import com.util.visible
import com.viewmodel.bookmark.BookmarkViewModel
import com.viewmodel.bookmark.BookmarkViewModelFactory
import kotlinx.coroutines.launch

class BookmarkFragment : Fragment(R.layout.fragment_bookmark) {

    private var _binding: FragmentBookmarkBinding? = null
    private val binding get() = _binding!!

    private val viewModel: BookmarkViewModel by viewModels {
        val app = requireActivity().application as com.NewsApp
        BookmarkViewModelFactory(app.repository)
    }

    private lateinit var adapter: NewsAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentBookmarkBinding.bind(view)

        setupToolbar()
        setupRecyclerView()
        observeUi()
    }

    private fun setupToolbar() {
        binding.toolbar.navigationIcon = AppCompatResources.getDrawable(
            requireContext(),
            androidx.appcompat.R.drawable.abc_ic_ab_back_material
        )
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadBookmarks()
    }

    private fun setupRecyclerView() {
        adapter = NewsAdapter(
            onItemClick = ::navigateToDetail,
            onBookmarkClick = { item -> viewModel.removeBookmark(item.articleUrl.orEmpty()) }
        )
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        val swipeCallback = object : ItemTouchHelper.SimpleCallback(
            0,
            ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val item = adapter.currentList.getOrNull(position) ?: return

                viewModel.removeBookmark(item.articleUrl.orEmpty())

                Snackbar.make(
                    binding.root,
                    R.string.bookmark_removed,
                    Snackbar.LENGTH_LONG
                ).setAction(R.string.undo) {
                    viewModel.restoreBookmark(item)
                }.show()
            }
        }

        ItemTouchHelper(swipeCallback).attachToRecyclerView(binding.recyclerView)
    }

    private fun observeUi() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    renderState(state)
                }
            }
        }
    }

    private fun renderState(state: UiState<List<NewsUiModel>>) {
        binding.progressBar.gone()
        binding.recyclerView.gone()
        binding.layoutEmpty.root.gone()
        binding.layoutError.root.gone()

        when (state) {
            is UiState.Loading -> binding.progressBar.visible()

            is UiState.Success -> {
                binding.recyclerView.visible()
                adapter.submitList(state.data)
            }

            is UiState.Empty -> {
                binding.layoutEmpty.root.visible()
                binding.layoutEmpty.tvEmptyTitle.text = getString(R.string.bookmark_empty_title)
                binding.layoutEmpty.tvEmptyMessage.text = getString(R.string.bookmark_empty_message)
                binding.layoutEmpty.btnTryOtherCity.gone()
            }

            is UiState.Error -> {
                binding.layoutError.root.visible()
                binding.layoutError.tvErrorMessage.text = state.message
            }

            is UiState.Idle -> Unit
        }
    }

    private fun navigateToDetail(item: NewsUiModel) {
        val safeUrl = item.articleUrl.orEmpty().trim()
        if (safeUrl.isBlank()) return
        if (findNavController().currentDestination?.id != R.id.bookmarkFragment) return

        findNavController().navigate(
            R.id.action_bookmarkFragment_to_detailFragment,
            item.toDetailBundle("bookmark")
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}