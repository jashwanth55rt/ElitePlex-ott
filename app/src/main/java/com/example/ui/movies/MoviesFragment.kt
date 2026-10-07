package com.example.ui.movies

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
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
import com.example.databinding.FragmentMoviesBinding
import com.example.ui.details.MovieDetailsActivity
import com.example.ui.viewmodel.MoviesUiState
import com.example.ui.viewmodel.MoviesViewModel
import com.google.android.material.chip.Chip
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MoviesFragment : Fragment() {

    private var _binding: FragmentMoviesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MoviesViewModel by lazy {
        val app = requireActivity().application as ElitePlexApplication
        ViewModelProvider(this, object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return MoviesViewModel(app.movieRepository) as T
            }
        })[MoviesViewModel::class.java]
    }

    private lateinit var movieAdapter: MovieGridAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMoviesBinding.inflate(inflater, container, false)
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
        val genres = listOf("All", "Anime", "Action", "Comedy", "Drama", "Sci-Fi", "Horror")
        binding.chipGroupGenres.removeAllViews()
        for ((index, g) in genres.withIndex()) {
            val chip = Chip(requireContext()).apply {
                text = g
                isCheckable = true
                isChecked = index == 0
                setEnsureMinTouchTargetSize(false)
                chipCornerRadius = resources.displayMetrics.density * 10
                updateChipColors(this, isChecked)
                setOnCheckedChangeListener { _, checked ->
                    updateChipColors(this, checked)
                    if (checked) {
                        viewModel.filterByGenre(g)
                    }
                }
            }
            binding.chipGroupGenres.addView(chip)
        }
    }

    private fun updateChipColors(chip: Chip, isChecked: Boolean) {
        val context = chip.context
        if (isChecked) {
            chip.setTextColor(ContextCompat.getColor(context, R.color.selected_text))
            chip.setChipBackgroundColorResource(R.color.selected_background)
            chip.setChipStrokeColorResource(R.color.selected_background)
            chip.chipStrokeWidth = 0f
        } else {
            chip.setTextColor(ContextCompat.getColor(context, R.color.chip_text_unselected))
            chip.setChipBackgroundColorResource(R.color.chip_bg_unselected)
            chip.setChipStrokeColorResource(R.color.border)
            chip.chipStrokeWidth = 1f
        }
    }

    private fun setupRecyclerView() {
        movieAdapter = MovieGridAdapter { movie ->
            openDetails(movie)
        }

        val gridLayoutManager = GridLayoutManager(requireContext(), 2)
        binding.rvMovies.apply {
            layoutManager = gridLayoutManager
            adapter = movieAdapter
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
        binding.swipeRefresh.setColorSchemeResources(R.color.accent_green)
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

    private fun renderState(state: MoviesUiState) {
        when (state) {
            is MoviesUiState.Loading -> {
                binding.pbLoading.visibility = View.VISIBLE
                binding.layoutError.visibility = View.GONE
                if (!binding.swipeRefresh.isRefreshing) {
                    binding.rvMovies.visibility = View.GONE
                }
                binding.pbPagination.visibility = View.GONE
            }
            is MoviesUiState.Success -> {
                binding.pbLoading.visibility = View.GONE
                binding.layoutError.visibility = View.GONE
                binding.rvMovies.visibility = View.VISIBLE
                binding.swipeRefresh.isRefreshing = false
                movieAdapter.submitList(state.items)
                binding.pbPagination.visibility = if (state.isLoadingMore) View.VISIBLE else View.GONE
            }
            is MoviesUiState.Error -> {
                binding.pbLoading.visibility = View.GONE
                binding.layoutError.visibility = View.VISIBLE
                binding.rvMovies.visibility = View.GONE
                binding.swipeRefresh.isRefreshing = false
                binding.tvErrorMessage.text = state.message
                binding.pbPagination.visibility = View.GONE
            }
        }
    }

    private fun openDetails(item: MovieItem) {
        val intent = Intent(requireContext(), MovieDetailsActivity::class.java).apply {
            putExtra(MovieDetailsActivity.EXTRA_ID, item.displayId)
            putExtra(MovieDetailsActivity.EXTRA_IS_TV, false)
            putExtra(MovieDetailsActivity.EXTRA_TITLE, item.displayTitle)
            val posterUrl = item.resolvedPoster ?: item.resolvedBackdrop
            putExtra(MovieDetailsActivity.EXTRA_POSTER, posterUrl)
            putExtra(MovieDetailsActivity.EXTRA_BACKDROP, item.resolvedBackdrop ?: posterUrl)
        }
        startActivity(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
