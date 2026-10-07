package com.example.ui.details

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
import com.example.ElitePlexApplication
import com.example.R
import com.example.data.model.MovieDetailResponse
import com.example.databinding.ActivityMovieDetailsBinding
import com.example.ui.home.HorizontalMovieAdapter
import com.example.ui.player.PlayerActivity
import com.example.ui.viewmodel.DetailsUiState
import com.example.ui.viewmodel.DetailsViewModel
import com.google.android.material.chip.Chip
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MovieDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMovieDetailsBinding

    private val viewModel: DetailsViewModel by lazy {
        val app = application as ElitePlexApplication
        ViewModelProvider(this, object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return DetailsViewModel(
                    movieRepository = app.movieRepository,
                    seriesRepository = app.seriesRepository,
                    savedRepository = app.savedRepository
                ) as T
            }
        })[DetailsViewModel::class.java]
    }

    private var contentId: String = ""
    private var isTv: Boolean = false
    private var contentTitle: String = ""
    private lateinit var episodesAdapter: EpisodesAdapter
    private lateinit var moreLikeThisAdapter: HorizontalMovieAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMovieDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        contentId = intent.getStringExtra(EXTRA_ID).orEmpty()
        isTv = intent.getBooleanExtra(EXTRA_IS_TV, false)
        contentTitle = intent.getStringExtra(EXTRA_TITLE).orEmpty()

        setupToolbar()
        setupAdapters()
        setupListeners()
        observeData()

        if (contentId.isNotEmpty()) {
            viewModel.loadDetails(contentId, isTv)
        }
    }

    private fun setupToolbar() {
        binding.btnBack.setOnClickListener { finish() }
        binding.tvToolbarTitle.text = contentTitle.ifEmpty { "Details" }

        binding.btnShare.setOnClickListener {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(
                    Intent.EXTRA_TEXT,
                    "Check out $contentTitle on ElitePlex! https://eliteplex-api.vercel.app/"
                )
            }
            startActivity(Intent.createChooser(shareIntent, "Share with"))
        }
    }

    private var currentPosterUrl: String? = null

    private fun setupAdapters() {
        episodesAdapter = EpisodesAdapter(
            onEpisodeClick = { episode ->
                val num = episode.episodeNumber ?: 1
                val currentState = viewModel.uiState.value as? DetailsUiState.Success
                val seasonNum = currentState?.selectedSeason ?: 1

                val intent = Intent(this, PlayerActivity::class.java).apply {
                    putExtra(PlayerActivity.EXTRA_CONTENT_ID, contentId)
                    putExtra(PlayerActivity.EXTRA_MEDIA_TYPE, "tv")
                    putExtra(PlayerActivity.EXTRA_TITLE, "$contentTitle: S$seasonNum E$num - ${episode.name ?: ""}")
                    putExtra(PlayerActivity.EXTRA_SEASON, seasonNum)
                    putExtra(PlayerActivity.EXTRA_EPISODE, num)
                }
                startActivity(intent)
            },
            onDownloadClick = { episode ->
                val num = episode.episodeNumber ?: 1
                val currentState = viewModel.uiState.value as? DetailsUiState.Success
                val seasonNum = currentState?.selectedSeason ?: 1

                showDownloadDialog(
                    title = "$contentTitle: S${seasonNum} E${num}",
                    subtitle = episode.name ?: "Episode $num",
                    season = seasonNum,
                    episode = num
                )
            }
        )
        binding.rvEpisodes.apply {
            layoutManager = LinearLayoutManager(this@MovieDetailsActivity)
            adapter = episodesAdapter
        }

        moreLikeThisAdapter = HorizontalMovieAdapter { item ->
            val intent = Intent(this, MovieDetailsActivity::class.java).apply {
                putExtra(EXTRA_ID, item.displayId)
                putExtra(EXTRA_IS_TV, item.isTvSeries)
                putExtra(EXTRA_TITLE, item.displayTitle)
            }
            startActivity(intent)
        }
        binding.rvMoreLikeThis.apply {
            layoutManager = LinearLayoutManager(this@MovieDetailsActivity, LinearLayoutManager.HORIZONTAL, false)
            adapter = moreLikeThisAdapter
        }
    }

    private fun setupListeners() {
        binding.btnWatchNow.setOnClickListener {
            val currentState = viewModel.uiState.value as? DetailsUiState.Success
            val mediaType = if (isTv || currentState?.detail?.isTvSeries == true) "tv" else "movie"
            val intent = Intent(this, PlayerActivity::class.java).apply {
                putExtra(PlayerActivity.EXTRA_CONTENT_ID, contentId)
                putExtra(PlayerActivity.EXTRA_MEDIA_TYPE, mediaType)
                putExtra(PlayerActivity.EXTRA_TITLE, contentTitle)
                if (mediaType == "tv") {
                    putExtra(PlayerActivity.EXTRA_SEASON, 1)
                    putExtra(PlayerActivity.EXTRA_EPISODE, 1)
                }
            }
            startActivity(intent)
        }

        binding.btnMyList.setOnClickListener {
            viewModel.toggleSaved()
        }

        binding.btnDownload.setOnClickListener {
            showDownloadDialog(
                title = contentTitle,
                subtitle = "Movie • Offline Download",
                season = 0,
                episode = 0
            )
        }

        binding.btnRetry.setOnClickListener {
            viewModel.loadDetails(contentId, isTv)
        }
    }

    private fun showDownloadDialog(title: String, subtitle: String?, season: Int, episode: Int) {
        val dialog = com.google.android.material.bottomsheet.BottomSheetDialog(this)
        val dialogBinding = com.example.databinding.DialogDownloadOptionsBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)

        dialogBinding.tvDialogTitle.text = "Download $title"
        dialogBinding.tvDialogSubtitle.text = subtitle ?: "Select quality for high-speed offline download"

        dialogBinding.pbDialogLoading.visibility = View.VISIBLE
        dialogBinding.rvDownloadOptions.visibility = View.GONE
        dialogBinding.tvDialogEmpty.visibility = View.GONE

        val app = application as ElitePlexApplication
        lifecycleScope.launch {
            val result = app.movieRepository.getDownloadOptions(
                contentId = contentId,
                mediaType = if (isTv) "tv" else "movie",
                season = season,
                episode = episode
            )

            dialogBinding.pbDialogLoading.visibility = View.GONE

            val options = result.getOrNull().orEmpty()
            if (options.isNotEmpty()) {
                dialogBinding.rvDownloadOptions.visibility = View.VISIBLE
                val adapter = com.example.ui.downloads.DownloadOptionsAdapter(options) { selectedOption ->
                    com.example.utils.DownloadHelper.startDownload(
                        context = this@MovieDetailsActivity,
                        contentId = contentId,
                        title = title,
                        subtitle = subtitle,
                        poster = currentPosterUrl,
                        mediaType = if (isTv) "tv" else "movie",
                        seasonNumber = season,
                        episodeNumber = episode,
                        downloadUrl = selectedOption.url,
                        quality = selectedOption.label
                    )
                    dialog.dismiss()
                }
                dialogBinding.rvDownloadOptions.apply {
                    layoutManager = LinearLayoutManager(this@MovieDetailsActivity)
                    this.adapter = adapter
                }
            } else {
                dialogBinding.tvDialogEmpty.visibility = View.VISIBLE
            }
        }

        dialog.show()
    }

    private fun observeData() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collectLatest { state ->
                    renderState(state)
                }
            }
        }
    }

    private fun renderState(state: DetailsUiState) {
        when (state) {
            is DetailsUiState.Loading -> {
                binding.pbLoading.visibility = View.VISIBLE
                binding.detailsScroll.visibility = View.GONE
                binding.layoutError.visibility = View.GONE
            }
            is DetailsUiState.Success -> {
                binding.pbLoading.visibility = View.GONE
                binding.layoutError.visibility = View.GONE
                binding.detailsScroll.visibility = View.VISIBLE

                bindDetails(state.detail, state.isSaved)
                moreLikeThisAdapter.submitList(state.moreLikeThis)

                if (state.detail.isTvSeries) {
                    binding.layoutTvSeries.visibility = View.VISIBLE
                    setupSeasonsChips(state.detail, state.selectedSeason)
                    episodesAdapter.submitList(state.episodes)
                } else {
                    binding.layoutTvSeries.visibility = View.GONE
                }
            }
            is DetailsUiState.Error -> {
                binding.pbLoading.visibility = View.GONE
                binding.detailsScroll.visibility = View.GONE
                binding.layoutError.visibility = View.VISIBLE
                binding.tvErrorMessage.text = state.message
            }
        }
    }

    private fun bindDetails(detail: MovieDetailResponse, isSaved: Boolean) {
        contentTitle = detail.displayTitle
        currentPosterUrl = detail.poster ?: detail.backdrop
        binding.tvToolbarTitle.text = contentTitle
        binding.tvTitle.text = detail.displayTitle
        binding.tvYear.text = detail.year ?: ""
        binding.tvRating.text = detail.formattedRating
        binding.tvOverview.text = detail.overview ?: "No synopsis available for this title."

        if (detail.formattedRuntime.isNotBlank()) {
            binding.tvRuntime.visibility = View.VISIBLE
            binding.tvRuntimeDot.visibility = View.VISIBLE
            binding.tvRuntime.text = detail.formattedRuntime
        } else {
            binding.tvRuntime.visibility = View.GONE
            binding.tvRuntimeDot.visibility = View.GONE
        }

        binding.tvGenres.text = detail.genresFormatted

        // Poster and Backdrop
        binding.ivBackdrop.load(detail.backdrop ?: detail.poster) {
            crossfade(true)
            placeholder(R.color.bg_surface)
            error(R.color.bg_surface)
        }

        binding.ivPoster.load(detail.poster ?: detail.backdrop) {
            crossfade(true)
            placeholder(R.drawable.bg_poster_placeholder)
            error(R.drawable.bg_poster_placeholder)
        }

        // My List button state
        updateMyListButton(isSaved)
    }

    private fun updateMyListButton(isSaved: Boolean) {
        if (isSaved) {
            binding.btnMyList.text = "In List"
            binding.btnMyList.setIconResource(R.drawable.ic_check)
            binding.btnMyList.setIconTintResource(R.color.accent_gold)
            binding.btnMyList.setTextColor(ContextCompat.getColor(this, R.color.accent_gold))
        } else {
            binding.btnMyList.text = "My List"
            binding.btnMyList.setIconResource(R.drawable.ic_add)
            binding.btnMyList.setIconTintResource(R.color.text_primary)
            binding.btnMyList.setTextColor(ContextCompat.getColor(this, R.color.text_primary))
        }
    }

    private fun setupSeasonsChips(detail: MovieDetailResponse, selectedSeason: Int) {
        val seasons = detail.seasons.orEmpty()
        if (seasons.isEmpty()) return

        if (binding.chipGroupSeasons.childCount != seasons.size) {
            binding.chipGroupSeasons.removeAllViews()
            for (season in seasons) {
                val sNum = season.seasonNumber ?: 1
                val chip = Chip(this).apply {
                    text = season.displayTitle
                    isCheckable = true
                    isChecked = sNum == selectedSeason
                    setTextColor(ContextCompat.getColor(context, R.color.text_primary))
                    setChipBackgroundColorResource(R.color.chip_bg)
                    setChipStrokeColorResource(R.color.card_stroke)
                    chipStrokeWidth = 1f
                    setOnClickListener {
                        viewModel.selectSeason(sNum)
                    }
                }
                binding.chipGroupSeasons.addView(chip)
            }
        }
    }

    companion object {
        const val EXTRA_ID = "extra_content_id"
        const val EXTRA_IS_TV = "extra_is_tv"
        const val EXTRA_TITLE = "extra_title"
    }
}
