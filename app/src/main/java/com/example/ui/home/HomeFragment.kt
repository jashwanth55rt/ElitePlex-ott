package com.example.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.ElitePlexApplication
import com.example.MainActivity
import com.example.R
import com.example.data.local.SavedItemEntity
import com.example.data.model.MovieItem
import com.example.data.model.StreamingPlatform
import com.example.databinding.FragmentHomeBinding
import com.example.databinding.ItemContentSectionBinding
import com.example.ui.details.MovieDetailsActivity
import com.example.ui.player.PlayerActivity
import com.example.ui.viewmodel.HomeUiState
import com.example.ui.viewmodel.HomeViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by lazy {
        val app = requireActivity().application as ElitePlexApplication
        ViewModelProvider(this, object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return HomeViewModel(app.movieRepository, app.searchRepository) as T
            }
        })[HomeViewModel::class.java]
    }

    private lateinit var continueWatchingAdapter: ContinueWatchingAdapter
    private var autoSlideJob: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupStreamingPlatforms()
        setupContinueWatching()
        setupListeners()
        observeData()
    }

    private fun setupStreamingPlatforms() {
        val platforms = listOf(
            StreamingPlatform("netflix", "Netflix", R.drawable.ic_platform_netflix, "netflix"),
            StreamingPlatform("prime", "Prime Video", R.drawable.ic_platform_prime, "prime"),
            StreamingPlatform("hotstar", "JioHotstar", R.drawable.ic_platform_hotstar, "hotstar"),
            StreamingPlatform("lionsgate", "Lionsgate Play", R.drawable.ic_platform_lionsgate, "lionsgate")
        )
        val adapter = StreamingPlatformAdapter(platforms) { platform ->
            (activity as? MainActivity)?.openSearchWithQuery(platform.name)
        }
        binding.rvStreamingPlatforms.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            this.adapter = adapter
        }
    }

    private fun setupContinueWatching() {
        continueWatchingAdapter = ContinueWatchingAdapter { historyItem ->
            val intent = Intent(requireContext(), PlayerActivity::class.java).apply {
                putExtra(PlayerActivity.EXTRA_CONTENT_ID, historyItem.tmdbId)
                putExtra(PlayerActivity.EXTRA_MEDIA_TYPE, historyItem.type)
                putExtra(PlayerActivity.EXTRA_TITLE, historyItem.title)
                putExtra(PlayerActivity.EXTRA_SEASON, historyItem.seasonNumber)
                putExtra(PlayerActivity.EXTRA_EPISODE, historyItem.episodeNumber)
                putExtra(PlayerActivity.EXTRA_RESUME_POS, historyItem.playbackPosition)
            }
            startActivity(intent)
        }
        binding.rvContinueWatching.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = continueWatchingAdapter
        }
    }

    private fun setupListeners() {
        binding.swipeRefresh.setColorSchemeResources(R.color.accent_gold)
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.loadHomeCatalog()
        }

        binding.btnRetry.setOnClickListener {
            viewModel.loadHomeCatalog()
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
                    viewModel.watchHistory.collectLatest { history ->
                        if (history.isNotEmpty()) {
                            binding.layoutContinueWatching.visibility = View.VISIBLE
                            continueWatchingAdapter.submitList(history)
                        } else {
                            binding.layoutContinueWatching.visibility = View.GONE
                        }
                    }
                }
            }
        }
    }

    private fun renderState(state: HomeUiState) {
        when (state) {
            is HomeUiState.Loading -> {
                binding.pbLoading.visibility = View.VISIBLE
                binding.layoutError.visibility = View.GONE
                if (!binding.swipeRefresh.isRefreshing) {
                    binding.layoutContent.visibility = View.GONE
                }
            }
            is HomeUiState.Success -> {
                binding.pbLoading.visibility = View.GONE
                binding.layoutError.visibility = View.GONE
                binding.layoutContent.visibility = View.VISIBLE
                binding.swipeRefresh.isRefreshing = false

                setupHeroSection(state.heroItems)
                setupContentSections(state)
            }
            is HomeUiState.Error -> {
                binding.pbLoading.visibility = View.GONE
                binding.layoutError.visibility = View.VISIBLE
                binding.layoutContent.visibility = View.GONE
                binding.swipeRefresh.isRefreshing = false
                binding.tvErrorMessage.text = state.message
            }
        }
    }

    private fun setupHeroSection(heroItems: List<MovieItem>) {
        if (heroItems.isEmpty()) {
            binding.heroViewPager.visibility = View.GONE
            autoSlideJob?.cancel()
            return
        }
        binding.heroViewPager.visibility = View.VISIBLE
        val app = requireActivity().application as ElitePlexApplication
        val adapter = HeroBannerAdapter(
            items = heroItems,
            onWatchClick = { item ->
                val intent = Intent(requireContext(), PlayerActivity::class.java).apply {
                    putExtra(PlayerActivity.EXTRA_CONTENT_ID, item.displayId)
                    putExtra(PlayerActivity.EXTRA_MEDIA_TYPE, if (item.isTvSeries) "tv" else "movie")
                    putExtra(PlayerActivity.EXTRA_TITLE, item.displayTitle)
                }
                startActivity(intent)
            },
            onDetailsClick = { item ->
                openDetails(item)
            },
            onBookmarkClick = { item ->
                lifecycleScope.launch {
                    val entity = SavedItemEntity(
                        id = item.displayId,
                        tmdbId = item.displayId,
                        title = item.displayTitle,
                        poster = item.poster,
                        backdrop = item.backdrop,
                        type = if (item.isTvSeries) "tv" else "movie",
                        rating = item.rating,
                        year = item.year,
                        overview = item.overview
                    )
                    val saved = app.savedRepository.toggleItem(entity)
                    val msg = if (saved) "Saved to Watchlist" else "Removed from Watchlist"
                    Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                }
            }
        )
        binding.heroViewPager.adapter = adapter
        startAutoSlide(heroItems.size)
    }

    private fun startAutoSlide(itemCount: Int) {
        autoSlideJob?.cancel()
        if (itemCount <= 1) return
        autoSlideJob = viewLifecycleOwner.lifecycleScope.launch {
            while (isActive) {
                delay(5000)
                if (_binding != null) {
                    val next = (binding.heroViewPager.currentItem + 1) % itemCount
                    binding.heroViewPager.setCurrentItem(next, true)
                }
            }
        }
    }

    private fun setupContentSections(state: HomeUiState.Success) {
        binding.sectionsContainer.removeAllViews()

        val allItems = (state.trending + state.popularMovies + state.topMovies)

        val sections = listOf(
            "Trending Movies" to state.trending,
            "Trending Movies - Cinema" to state.popularMovies.reversed().take(10),
            "Popular Movies" to state.popularMovies,
            "Latest Movies" to state.popularMovies.take(10),
            "Top Rated Movies" to state.topMovies,
            "Popular TV Series" to state.popularTv,
            "Top Rated TV Series" to state.topTv,
            "Trending Anime & Animation" to state.trendingAnime,
            "Action & Adventure" to allItems.filter { it.displayTitle.contains("man", true) || it.displayTitle.contains("war", true) || it.displayTitle.contains("avatar", true) }.take(8),
            "Recommended For You" to state.trending.reversed().take(10)
        )

        for ((title, items) in sections) {
            if (items.isNotEmpty()) {
                addSectionView(title, items)
            }
        }
    }

    private fun addSectionView(title: String, items: List<MovieItem>) {
        val sectionBinding = ItemContentSectionBinding.inflate(
            layoutInflater, binding.sectionsContainer, false
        )
        sectionBinding.tvSectionTitle.text = title

        val adapter = HorizontalMovieAdapter { item ->
            openDetails(item)
        }
        sectionBinding.rvSectionItems.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            this.adapter = adapter
        }
        adapter.submitList(items)

        binding.sectionsContainer.addView(sectionBinding.root)
    }

    private fun openDetails(item: MovieItem) {
        val intent = Intent(requireContext(), MovieDetailsActivity::class.java).apply {
            putExtra(MovieDetailsActivity.EXTRA_ID, item.displayId)
            putExtra(MovieDetailsActivity.EXTRA_IS_TV, item.isTvSeries)
            putExtra(MovieDetailsActivity.EXTRA_TITLE, item.displayTitle)
        }
        startActivity(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        autoSlideJob?.cancel()
        _binding = null
    }
}
