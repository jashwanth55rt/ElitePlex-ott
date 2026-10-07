package com.example

import android.app.Application
import com.example.data.api.ApiClient
import com.example.data.local.AppDatabase
import com.example.data.repository.MovieRepository
import com.example.data.repository.SavedRepository
import com.example.data.repository.SearchRepository
import com.example.data.repository.SeriesRepository

class ElitePlexApplication : Application() {

    val database by lazy { AppDatabase.getDatabase(this) }

    val movieRepository by lazy {
        MovieRepository(
            apiService = ApiClient.apiService,
            watchHistoryDao = database.watchHistoryDao(),
            savedItemDao = database.savedItemDao()
        )
    }

    val seriesRepository by lazy {
        SeriesRepository(apiService = ApiClient.apiService)
    }

    val searchRepository by lazy {
        SearchRepository(apiService = ApiClient.apiService)
    }

    val savedRepository by lazy {
        SavedRepository(savedItemDao = database.savedItemDao())
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: ElitePlexApplication
            private set
    }
}
