package com.example.ui.search

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import com.example.ElitePlexApplication
import com.example.MainActivity
import com.example.R
import com.example.data.model.MovieItem
import com.example.databinding.FragmentSearchBinding
import com.example.ui.details.MovieDetailsActivity
import com.example.ui.movies.MovieGridAdapter
import com.example.ui.viewmodel.SearchUiState
import com.example.ui.viewmodel.SearchViewModel
import com.google.android.material.chip.Chip
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class SearchFragment : Fragment() {

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SearchViewModel by lazy {
        val app = requireActivity().application as ElitePlexApplication
        ViewModelProvider(this, object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SearchViewModel(app.searchRepository) as T
            }
        })[SearchViewModel::class.java]
    }

    private lateinit var searchAdapter: MovieGridAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupSearchInput()
        setupGenreFilterChips()
        observeData()

        binding.btnSearchBack.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setupRecyclerView() {
        searchAdapter = MovieGridAdapter { item ->
            openDetails(item)
        }
        binding.rvSearchResults.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter = searchAdapter
        }
    }

    private fun setupGenreFilterChips() {
        val genres = listOf("All", "Action", "Comedy", "Drama", "Sci-Fi", "Horror", "Animation")
        binding.chipGroupSearchGenres.removeAllViews()
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
                    if (checked && g != "All") {
                        binding.etSearch.setText(g)
                        binding.etSearch.setSelection(g.length)
                        viewModel.submitSearch(g)
                    }
                }
            }
            binding.chipGroupSearchGenres.addView(chip)
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

    private fun setupSearchInput() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val text = s?.toString().orEmpty()
                binding.btnClearSearch.visibility = if (text.isNotEmpty()) View.VISIBLE else View.GONE
                if (text.isNotBlank()) {
                    viewModel.onQueryChanged(text)
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.etSearch.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val query = v.text.toString().trim()
                if (query.isNotEmpty()) {
                    viewModel.submitSearch(query)
                }
                true
            } else false
        }

        binding.btnClearSearch.setOnClickListener {
            binding.etSearch.setText("")
        }

        binding.btnClearRecent.setOnClickListener {
            viewModel.clearRecentSearches()
        }
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collectLatest { state ->
                        renderState(state)
                    }
                }
                launch {
                    viewModel.recentSearches.collectLatest { searches ->
                        renderRecentSearches(searches)
                    }
                }
            }
        }
    }

    private fun renderRecentSearches(searches: List<String>) {
        val trendingList = listOf(
            "Spider-Man: Brand New Day",
            "Drishyam",
            "Naruto",
            "Toxic",
            "Insidious: Out of the Further",
            "Fall 2: Deadpoint",
            "Lanterns",
            "Digger",
            "Resident Evil"
        )
        val combined = (searches + trendingList).distinct()

        binding.chipGroupRecent.removeAllViews()
        for (query in combined) {
            val chip = Chip(requireContext()).apply {
                text = query
                isClickable = true
                isCheckable = false
                setEnsureMinTouchTargetSize(false)
                chipCornerRadius = resources.displayMetrics.density * 10
                setTextColor(ContextCompat.getColor(context, R.color.chip_text_unselected))
                setChipBackgroundColorResource(R.color.chip_bg_unselected)
                setChipStrokeColorResource(R.color.border)
                chipStrokeWidth = 1f
                setOnClickListener {
                    binding.etSearch.setText(query)
                    binding.etSearch.setSelection(query.length)
                    viewModel.submitSearch(query)
                }
            }
            binding.chipGroupRecent.addView(chip)
        }
        binding.layoutRecentSearches.visibility = View.VISIBLE
    }

    private fun renderState(state: SearchUiState) {
        when (state) {
            is SearchUiState.Idle -> {
                binding.pbLoading.visibility = View.GONE
                binding.rvSearchResults.visibility = View.GONE
                binding.layoutEmpty.visibility = View.GONE
                binding.layoutError.visibility = View.GONE
                binding.layoutRecentSearches.visibility = View.VISIBLE
            }
            is SearchUiState.Loading -> {
                binding.pbLoading.visibility = View.VISIBLE
                binding.rvSearchResults.visibility = View.GONE
                binding.layoutEmpty.visibility = View.GONE
                binding.layoutError.visibility = View.GONE
                binding.layoutRecentSearches.visibility = View.GONE
            }
            is SearchUiState.Success -> {
                binding.pbLoading.visibility = View.GONE
                binding.rvSearchResults.visibility = View.VISIBLE
                binding.layoutEmpty.visibility = View.GONE
                binding.layoutError.visibility = View.GONE
                binding.layoutRecentSearches.visibility = View.GONE
                searchAdapter.submitList(state.results)
            }
            is SearchUiState.Empty -> {
                binding.pbLoading.visibility = View.GONE
                binding.rvSearchResults.visibility = View.GONE
                binding.layoutEmpty.visibility = View.VISIBLE
                binding.layoutError.visibility = View.GONE
                binding.layoutRecentSearches.visibility = View.GONE
                binding.tvEmptyMessage.text = "No movies or series found for \"${state.query}\"."
            }
            is SearchUiState.Error -> {
                binding.pbLoading.visibility = View.GONE
                binding.rvSearchResults.visibility = View.GONE
                binding.layoutEmpty.visibility = View.GONE
                binding.layoutError.visibility = View.VISIBLE
                binding.layoutRecentSearches.visibility = View.GONE
                binding.tvErrorMessage.text = state.message
            }
        }
    }

    fun searchForQuery(query: String) {
        binding.etSearch.setText(query)
        binding.etSearch.setSelection(query.length)
        viewModel.submitSearch(query)
    }

    private fun openDetails(item: MovieItem) {
        val intent = Intent(requireContext(), MovieDetailsActivity::class.java).apply {
            putExtra(MovieDetailsActivity.EXTRA_ID, item.displayId)
            putExtra(MovieDetailsActivity.EXTRA_IS_TV, item.isTvSeries)
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
