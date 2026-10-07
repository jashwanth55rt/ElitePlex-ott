package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.databinding.ActivityMainBinding
import com.example.ui.home.HomeFragment
import com.example.ui.movies.MoviesFragment
import com.example.ui.mylist.MyListFragment
import com.example.ui.player.PlayerActivity
import com.example.ui.search.SearchFragment
import com.example.ui.series.SeriesFragment
import com.example.ui.settings.SettingsFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val homeFragment by lazy { HomeFragment() }
    private val moviesFragment by lazy { MoviesFragment() }
    private val seriesFragment by lazy { SeriesFragment() }
    private val searchFragment by lazy { SearchFragment() }
    private val myListFragment by lazy { MyListFragment() }
    private val settingsFragment by lazy { SettingsFragment() }

    private var activeFragment: Fragment = homeFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupFragments(savedInstanceState)
        setupNavigation()
        setupBackHandler()
    }

    private fun setupFragments(savedInstanceState: Bundle?) {
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .add(R.id.fragment_container, homeFragment, TAG_HOME)
                .commit()
            activeFragment = homeFragment
        }
    }

    private fun setupNavigation() {
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    switchFragment(homeFragment, TAG_HOME)
                    true
                }
                R.id.nav_movies -> {
                    switchFragment(moviesFragment, TAG_MOVIES)
                    true
                }
                R.id.nav_play -> {
                    // Launch Media3 player or continue watching
                    val intent = Intent(this, PlayerActivity::class.java).apply {
                        putExtra(PlayerActivity.EXTRA_CONTENT_ID, "969681")
                        putExtra(PlayerActivity.EXTRA_MEDIA_TYPE, "movie")
                        putExtra(PlayerActivity.EXTRA_TITLE, "Spider-Man: Brand New Day")
                    }
                    startActivity(intent)
                    false // Keep previous selected tab active
                }
                R.id.nav_series -> {
                    switchFragment(seriesFragment, TAG_SERIES)
                    true
                }
                R.id.nav_settings -> {
                    switchFragment(settingsFragment, TAG_SETTINGS)
                    true
                }
                else -> false
            }
        }

        binding.btnFloatingMenu.setOnClickListener {
            showQuickMenuDialog()
        }
    }

    private fun showQuickMenuDialog() {
        val bottomSheet = com.google.android.material.bottomsheet.BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.dialog_download_options, null) as android.widget.LinearLayout
        view.removeAllViews()
        view.setPadding(48, 36, 48, 48)

        val title = android.widget.TextView(this).apply {
            text = "ElitePlex Quick Menu"
            textSize = 20f
            setTextColor(getColor(R.color.accent_green))
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setPadding(0, 0, 0, 32)
        }
        view.addView(title)

        fun createMenuItem(label: String, iconRes: Int, onClick: () -> Unit) {
            val itemLayout = android.widget.LinearLayout(this).apply {
                orientation = android.widget.LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                setPadding(24, 28, 24, 28)
                setBackgroundResource(R.drawable.bg_card_24)
                val params = android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                )
                params.bottomMargin = 16
                layoutParams = params
                isClickable = true
                isFocusable = true
                setOnClickListener {
                    bottomSheet.dismiss()
                    onClick()
                }
            }
            val icon = android.widget.ImageView(this).apply {
                setImageResource(iconRes)
                imageTintList = android.content.res.ColorStateList.valueOf(getColor(R.color.accent_green))
                val ip = android.widget.LinearLayout.LayoutParams(54, 54)
                ip.marginEnd = 32
                layoutParams = ip
            }
            val text = android.widget.TextView(this).apply {
                text = label
                textSize = 15f
                setTextColor(getColor(R.color.text_primary))
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            }
            itemLayout.addView(icon)
            itemLayout.addView(text)
            view.addView(itemLayout)
        }

        createMenuItem("Search Movies & Series", R.drawable.ic_search) {
            navigateToSearch()
        }
        createMenuItem("My List & Downloads", R.drawable.ic_bookmark) {
            switchFragment(myListFragment, TAG_MYLIST)
        }
        createMenuItem("Browse Anime Collection", R.drawable.ic_movie) {
            openSearchWithQuery("Anime")
        }
        createMenuItem("App Settings & Storage", R.drawable.ic_settings) {
            binding.bottomNav.selectedItemId = R.id.nav_settings
        }

        bottomSheet.setContentView(view)
        bottomSheet.show()
    }

    fun navigateToSearch() {
        switchFragment(searchFragment, TAG_SEARCH)
    }

    fun openSearchWithQuery(query: String) {
        switchFragment(searchFragment, TAG_SEARCH)
        searchFragment.searchForQuery(query)
    }

    private fun switchFragment(targetFragment: Fragment, tag: String) {
        if (activeFragment == targetFragment) return

        val transaction = supportFragmentManager.beginTransaction()

        if (!targetFragment.isAdded) {
            transaction.hide(activeFragment).add(R.id.fragment_container, targetFragment, tag)
        } else {
            transaction.hide(activeFragment).show(targetFragment)
        }

        activeFragment = targetFragment
        transaction.commit()
    }

    private fun setupBackHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (activeFragment != homeFragment) {
                    binding.bottomNav.selectedItemId = R.id.nav_home
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    companion object {
        private const val TAG_HOME = "tag_home"
        private const val TAG_MOVIES = "tag_movies"
        private const val TAG_SERIES = "tag_series"
        private const val TAG_SEARCH = "tag_search"
        private const val TAG_MYLIST = "tag_mylist"
        private const val TAG_SETTINGS = "tag_settings"
    }
}
