package com.example.ui.mylist

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
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.ElitePlexApplication
import com.example.R
import com.example.databinding.FragmentMyListBinding
import com.example.ui.details.MovieDetailsActivity
import com.example.ui.downloads.DownloadsAdapter
import com.example.ui.player.PlayerActivity
import com.example.ui.viewmodel.MyListViewModel
import com.example.utils.DownloadHelper
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MyListFragment : Fragment() {

    private var _binding: FragmentMyListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MyListViewModel by lazy {
        val app = requireActivity().application as ElitePlexApplication
        ViewModelProvider(this, object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return MyListViewModel(
                    savedRepository = app.savedRepository,
                    downloadDao = app.database.downloadDao()
                ) as T
            }
        })[MyListViewModel::class.java]
    }

    private lateinit var myListAdapter: MyListAdapter
    private lateinit var downloadsAdapter: DownloadsAdapter
    private var isWatchlistSelected = true

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMyListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerViews()
        setupTabs()
        observeData()
    }

    private fun setupRecyclerViews() {
        myListAdapter = MyListAdapter { savedItem ->
            val intent = Intent(requireContext(), MovieDetailsActivity::class.java).apply {
                putExtra(MovieDetailsActivity.EXTRA_ID, savedItem.id)
                putExtra(MovieDetailsActivity.EXTRA_IS_TV, savedItem.type.equals("tv", ignoreCase = true))
                putExtra(MovieDetailsActivity.EXTRA_TITLE, savedItem.title)
            }
            startActivity(intent)
        }
        binding.rvMyList.apply {
            layoutManager = GridLayoutManager(requireContext(), 3)
            adapter = myListAdapter
        }

        downloadsAdapter = DownloadsAdapter(
            onPlayOfflineClick = { downloadItem ->
                val intent = Intent(requireContext(), PlayerActivity::class.java).apply {
                    putExtra(PlayerActivity.EXTRA_CONTENT_ID, downloadItem.contentId)
                    putExtra(PlayerActivity.EXTRA_TITLE, downloadItem.title)
                    putExtra(PlayerActivity.EXTRA_MEDIA_TYPE, downloadItem.mediaType)
                    putExtra(PlayerActivity.EXTRA_OFFLINE_URI, downloadItem.localUri ?: downloadItem.downloadUrl)
                }
                startActivity(intent)
            },
            onDeleteClick = { downloadItem ->
                DownloadHelper.cancelOrDeleteDownload(requireContext(), downloadItem)
            }
        )
        binding.rvDownloads.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = downloadsAdapter
        }
    }

    private fun setupTabs() {
        binding.tabWatchlist.setOnClickListener {
            selectTab(isWatchlist = true)
        }

        binding.tabDownloads.setOnClickListener {
            selectTab(isWatchlist = false)
        }

        selectTab(isWatchlist = true)
    }

    private fun selectTab(isWatchlist: Boolean) {
        isWatchlistSelected = isWatchlist
        val context = requireContext()

        if (isWatchlist) {
            binding.tabWatchlist.setBackgroundResource(R.drawable.bg_button_gold)
            binding.tabWatchlist.setTextColor(ContextCompat.getColor(context, R.color.accent_gold_dark))
            binding.tabDownloads.setBackgroundResource(R.color.transparent)
            binding.tabDownloads.setTextColor(ContextCompat.getColor(context, R.color.text_secondary))

            binding.containerWatchlist.visibility = View.VISIBLE
            binding.containerDownloads.visibility = View.GONE

            val count = viewModel.savedItems.value.size
            binding.tvHeaderBadge.text = "$count ${if (count == 1) "title" else "titles"}"
        } else {
            binding.tabDownloads.setBackgroundResource(R.drawable.bg_button_gold)
            binding.tabDownloads.setTextColor(ContextCompat.getColor(context, R.color.accent_gold_dark))
            binding.tabWatchlist.setBackgroundResource(R.color.transparent)
            binding.tabWatchlist.setTextColor(ContextCompat.getColor(context, R.color.text_secondary))

            binding.containerWatchlist.visibility = View.GONE
            binding.containerDownloads.visibility = View.VISIBLE

            val count = viewModel.downloads.value.size
            binding.tvHeaderBadge.text = "$count ${if (count == 1) "video" else "videos"}"
        }
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.savedItems.collectLatest { items ->
                        myListAdapter.submitList(items)
                        if (items.isEmpty()) {
                            binding.layoutEmptyWatchlist.visibility = View.VISIBLE
                            binding.rvMyList.visibility = View.GONE
                        } else {
                            binding.layoutEmptyWatchlist.visibility = View.GONE
                            binding.rvMyList.visibility = View.VISIBLE
                        }
                        if (isWatchlistSelected) {
                            binding.tvHeaderBadge.text = "${items.size} ${if (items.size == 1) "title" else "titles"}"
                        }
                    }
                }

                launch {
                    viewModel.downloads.collectLatest { downloads ->
                        downloadsAdapter.submitList(downloads)
                        if (downloads.isEmpty()) {
                            binding.layoutEmptyDownloads.visibility = View.VISIBLE
                            binding.rvDownloads.visibility = View.GONE
                        } else {
                            binding.layoutEmptyDownloads.visibility = View.GONE
                            binding.rvDownloads.visibility = View.VISIBLE
                        }
                        if (!isWatchlistSelected) {
                            binding.tvHeaderBadge.text = "${downloads.size} ${if (downloads.size == 1) "video" else "videos"}"
                        }
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
