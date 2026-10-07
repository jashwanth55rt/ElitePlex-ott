package com.example.ui.series

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.ElitePlexApplication
import com.example.R
import com.example.data.model.MovieItem
import com.example.databinding.FragmentSeriesBinding
import com.example.ui.details.MovieDetailsActivity
import com.example.ui.movies.MovieGridAdapter
import com.example.ui.viewmodel.SeriesUiState
import com.example.ui.viewmodel.SeriesViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class SeriesFragment : Fragment() {

    private var _binding: FragmentSeriesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SeriesViewModel by lazy {
        val app = requireActivity().application as ElitePlexApplication
        ViewModelProvider(this, object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SeriesViewModel(app.seriesRepository) as T
            }
        })[SeriesViewModel::class.java]
    }

    private lateinit var seriesAdapter: MovieGridAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSeriesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupGenreChips()
        setupRecyclerView()
        setupListeners()
        observeData()
    }

    private fun setupGenreChips() {
        val genres = listOf("All", "Drama", "Action", "Anime", "Sci-Fi", "Comedy", "Crime", "Animation")
        binding.chipGroupGenres.removeAllViews()
        for ((index, g) in genres.withIndex()) {
            val chip = com.google.android.material.chip.Chip(requireContext()).apply {
                text = g
                isCheckable = true
                isChecked = index == 0
                setTextColor(androidx.core.content.ContextCompat.getColor(context, R.color.text_primary))
                setChipBackgroundColorResource(R.color.bg_card)
                setChipStrokeColorResource(R.color.border)
                chipStrokeWidth = 1f
                setOnClickListener {
                    viewModel.refresh()
                }
            }
            binding.chipGroupGenres.addView(chip)
        }
    }

    private fun setupRecyclerView() {
        seriesAdapter = MovieGridAdapter { show ->
            openDetails(show)
        }

        val gridLayoutManager = GridLayoutManager(requireContext(), 2)
        binding.rvSeries.apply {
            layoutManager = gridLayoutManager
            adapter = seriesAdapter
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    if (dy > 0) {
                        val visibleItemCount = gridLayoutManager.childCount
                        val totalItemCount = gridLayoutManager.itemCount
                        val firstVisibleItemPosition = gridLayoutManager.findFirstVisibleItemPosition()

                        if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount - 6) {
                            viewModel.loadNextPage()
                        }
                    }
                }
            })
        }
    }

    private fun setupListeners() {
        binding.swipeRefresh.setColorSchemeResources(R.color.accent_gold)
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refresh()
        }

        binding.btnRetry.setOnClickListener {
            viewModel.refresh()
        }
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collectLatest { state ->
                    renderState(state)
                }
            }
        }
    }

    private fun renderState(state: SeriesUiState) {
        when (state) {
            is SeriesUiState.Loading -> {
                binding.pbLoading.visibility = View.VISIBLE
                binding.layoutError.visibility = View.GONE
                if (!binding.swipeRefresh.isRefreshing) {
                    binding.rvSeries.visibility = View.GONE
                }
                binding.pbPagination.visibility = View.GONE
            }
            is SeriesUiState.Success -> {
                binding.pbLoading.visibility = View.GONE
                binding.layoutError.visibility = View.GONE
                binding.rvSeries.visibility = View.VISIBLE
                binding.swipeRefresh.isRefreshing = false
                seriesAdapter.submitList(state.items)
                binding.pbPagination.visibility = if (state.isLoadingMore) View.VISIBLE else View.GONE
            }
            is SeriesUiState.Error -> {
                binding.pbLoading.visibility = View.GONE
                binding.layoutError.visibility = View.VISIBLE
                binding.rvSeries.visibility = View.GONE
                binding.swipeRefresh.isRefreshing = false
                binding.tvErrorMessage.text = state.message
                binding.pbPagination.visibility = View.GONE
            }
        }
    }

    private fun openDetails(item: MovieItem) {
        val intent = Intent(requireContext(), MovieDetailsActivity::class.java).apply {
            putExtra(MovieDetailsActivity.EXTRA_ID, item.displayId)
            putExtra(MovieDetailsActivity.EXTRA_IS_TV, true)
            putExtra(MovieDetailsActivity.EXTRA_TITLE, item.displayTitle)
        }
        startActivity(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
