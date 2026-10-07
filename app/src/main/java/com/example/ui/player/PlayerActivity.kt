package com.example.ui.player

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.ElitePlexApplication
import com.example.data.local.WatchHistoryEntity
import com.example.data.model.PlayResponse
import com.example.data.model.PlaySource
import com.example.databinding.ActivityPlayerBinding
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class PlayerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPlayerBinding

    private var exoPlayer: ExoPlayer? = null
    private var contentId: String = ""
    private var mediaType: String = "movie"
    private var titleText: String = ""
    private var seasonNum: Int = 0
    private var episodeNum: Int = 0
    private var resumePosition: Long = 0L

    private var availableSources: List<PlaySource> = emptyList()
    private var currentServerIndex: Int = 0
    private var progressTrackingJob: Job? = null

    private val movieRepository by lazy {
        (application as ElitePlexApplication).movieRepository
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        contentId = intent.getStringExtra(EXTRA_CONTENT_ID).orEmpty()
        mediaType = intent.getStringExtra(EXTRA_MEDIA_TYPE) ?: "movie"
        titleText = intent.getStringExtra(EXTRA_TITLE).orEmpty()
        seasonNum = intent.getIntExtra(EXTRA_SEASON, 0)
        episodeNum = intent.getIntExtra(EXTRA_EPISODE, 0)
        resumePosition = intent.getLongExtra(EXTRA_RESUME_POS, 0L)

        setupUI()

        val offlineUri = intent.getStringExtra(EXTRA_OFFLINE_URI)
        if (!offlineUri.isNullOrBlank()) {
            binding.btnSwitchServer.visibility = View.GONE
            playWithExoPlayer(offlineUri)
        } else {
            loadPlaybackSources()
        }
    }

    private fun setupUI() {
        binding.tvPlayerTitle.text = titleText.ifEmpty { "ElitePlex Player" }
        binding.btnPlayerBack.setOnClickListener { finish() }

        binding.btnSwitchServer.setOnClickListener {
            val isVisible = binding.layoutServerStrip.visibility == View.VISIBLE
            binding.layoutServerStrip.visibility = if (isVisible) View.GONE else View.VISIBLE
        }

        binding.btnErrorRetry.setOnClickListener {
            if (availableSources.isNotEmpty()) {
                playServer(currentServerIndex)
            } else {
                loadPlaybackSources()
            }
        }

        binding.btnErrorNextServer.setOnClickListener {
            playNextServer()
        }
    }

    private var webView: WebView? = null

    @SuppressLint("SetJavaScriptEnabled")
    private fun getOrCreateWebView(): WebView {
        if (webView == null) {
            webView = WebView(this).apply {
                layoutParams = android.view.ViewGroup.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT
                )
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.mediaPlaybackRequiresUserGesture = false
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true
                settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                webViewClient = WebViewClient()
                webChromeClient = WebChromeClient()
            }
            binding.webViewContainer.addView(webView)
        }
        return webView!!
    }

    private fun loadPlaybackSources() {
        binding.pbPlayerLoading.visibility = View.VISIBLE
        binding.layoutPlayerError.visibility = View.GONE

        lifecycleScope.launch {
            // Check for saved resume position in database if not passed
            if (resumePosition == 0L) {
                val history = movieRepository.getWatchHistoryItem(contentId)
                if (history != null && history.playbackPosition > 5000) {
                    resumePosition = history.playbackPosition
                }
            }

            var result = movieRepository.getPlaySources(
                tmdbId = contentId,
                media = mediaType,
                season = seasonNum,
                episode = episodeNum
            )

            if (result.isFailure || result.getOrNull()?.sources.isNullOrEmpty()) {
                val altMedia = if (mediaType == "tv") "movie" else "tv"
                val altResult = movieRepository.getPlaySources(
                    tmdbId = contentId,
                    media = altMedia,
                    season = seasonNum,
                    episode = episodeNum
                )
                if (altResult.isSuccess && !altResult.getOrNull()?.sources.isNullOrEmpty()) {
                    result = altResult
                    mediaType = altMedia
                }
            }

            binding.pbPlayerLoading.visibility = View.GONE

            result.onSuccess { playResponse ->
                handlePlayResponse(playResponse)
            }.onFailure { error ->
                showPlaybackError("Playback unavailable for this title.", error.localizedMessage)
            }
        }
    }

    private fun handlePlayResponse(playResponse: PlayResponse) {
        val sources = playResponse.sources.orEmpty()
        if (sources.isEmpty()) {
            showPlaybackError("Playback unavailable for this title.", "No authorized streaming servers found.")
            return
        }

        availableSources = sources
        setupServerList()
        currentServerIndex = 0
        playServer(0)
    }

    private fun setupServerList() {
        val serverAdapter = ServerAdapter(availableSources, currentServerIndex) { index, _ ->
            playServer(index)
            binding.layoutServerStrip.visibility = View.GONE
        }
        binding.rvServers.apply {
            layoutManager = LinearLayoutManager(this@PlayerActivity, LinearLayoutManager.HORIZONTAL, false)
            adapter = serverAdapter
        }
    }

    private fun playServer(index: Int) {
        if (index < 0 || index >= availableSources.size) {
            showPlaybackError("Playback unavailable for this title.", "Server index out of range.")
            return
        }

        currentServerIndex = index
        val server = availableSources[index]
        binding.btnSwitchServer.text = server.displayLabel
        (binding.rvServers.adapter as? ServerAdapter)?.setSelectedIndex(index)

        binding.layoutPlayerError.visibility = View.GONE

        val streamUrl = server.bestUrl
        if (streamUrl.isBlank()) {
            playNextServer()
            return
        }

        if (server.isDirectStream) {
            playWithExoPlayer(streamUrl)
        } else {
            playWithWebView(streamUrl)
        }
    }

    private fun playWithExoPlayer(url: String) {
        binding.webViewContainer.visibility = View.GONE
        webView?.loadUrl("about:blank")
        binding.playerView.visibility = View.VISIBLE
        binding.pbPlayerLoading.visibility = View.VISIBLE

        releaseExoPlayer()

        exoPlayer = ExoPlayer.Builder(this).build().apply {
            binding.playerView.player = this
            val mediaItem = MediaItem.fromUri(url)
            setMediaItem(mediaItem)
            if (resumePosition > 0) {
                seekTo(resumePosition)
            }
            prepare()
            playWhenReady = true

            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    when (playbackState) {
                        Player.STATE_BUFFERING -> binding.pbPlayerLoading.visibility = View.VISIBLE
                        Player.STATE_READY -> binding.pbPlayerLoading.visibility = View.GONE
                        Player.STATE_ENDED -> binding.pbPlayerLoading.visibility = View.GONE
                        Player.STATE_IDLE -> Unit
                    }
                }

                override fun onPlayerError(error: PlaybackException) {
                    binding.pbPlayerLoading.visibility = View.GONE
                    playNextServer()
                }
            })
        }

        startProgressTracking()
    }

    private fun playWithWebView(url: String) {
        releaseExoPlayer()
        binding.playerView.visibility = View.GONE
        binding.playerView.player = null
        binding.webViewContainer.visibility = View.VISIBLE
        binding.pbPlayerLoading.visibility = View.GONE

        getOrCreateWebView().loadUrl(url)
    }

    private fun playNextServer() {
        if (currentServerIndex < availableSources.size - 1) {
            playServer(currentServerIndex + 1)
        } else {
            showPlaybackError(
                "Playback unavailable for this title.",
                "All streaming servers have been exhausted."
            )
        }
    }

    private fun showPlaybackError(title: String, subtitle: String?) {
        binding.layoutPlayerError.visibility = View.VISIBLE
        binding.tvPlayerError.text = title
        binding.tvPlayerErrorSub.text = subtitle ?: "Please try again later."
        binding.btnErrorNextServer.visibility =
            if (currentServerIndex < availableSources.size - 1) View.VISIBLE else View.GONE
    }

    private fun startProgressTracking() {
        progressTrackingJob?.cancel()
        progressTrackingJob = lifecycleScope.launch {
            while (isActive) {
                delay(3000)
                saveCurrentWatchProgress()
            }
        }
    }

    private fun saveCurrentWatchProgress() {
        val player = exoPlayer ?: return
        val pos = player.currentPosition
        val duration = player.duration
        if (pos > 0 && duration > 0) {
            lifecycleScope.launch {
                val entity = WatchHistoryEntity(
                    id = contentId,
                    tmdbId = contentId,
                    title = titleText,
                    poster = null,
                    backdrop = null,
                    type = mediaType,
                    seasonNumber = seasonNum,
                    episodeNumber = episodeNum,
                    playbackPosition = pos,
                    duration = duration,
                    lastWatchedAt = System.currentTimeMillis()
                )
                movieRepository.saveWatchProgress(entity)
            }
        }
    }

    private fun releaseExoPlayer() {
        progressTrackingJob?.cancel()
        exoPlayer?.let { player ->
            saveCurrentWatchProgress()
            player.release()
        }
        exoPlayer = null
    }

    override fun onPause() {
        super.onPause()
        exoPlayer?.pause()
        saveCurrentWatchProgress()
    }

    override fun onStop() {
        super.onStop()
        releaseExoPlayer()
    }

    override fun onDestroy() {
        super.onDestroy()
        releaseExoPlayer()
        webView?.let {
            it.destroy()
            binding.webViewContainer.removeAllViews()
        }
        webView = null
    }

    companion object {
        const val EXTRA_CONTENT_ID = "extra_content_id"
        const val EXTRA_MEDIA_TYPE = "extra_media_type"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_SEASON = "extra_season"
        const val EXTRA_EPISODE = "extra_episode"
        const val EXTRA_RESUME_POS = "extra_resume_pos"
        const val EXTRA_OFFLINE_URI = "extra_offline_uri"
    }
}
