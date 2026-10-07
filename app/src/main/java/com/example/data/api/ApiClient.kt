package com.example.data.api

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializer
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    private val loggingInterceptor by lazy {
        HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
    }

    private val okHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    private val gson: Gson by lazy {
        GsonBuilder()
            .setLenient()
            .registerTypeAdapter(Double::class.javaObjectType, JsonDeserializer { json, _, _ ->
                if (json == null || json.isJsonNull) null
                else try {
                    val s = json.asString.trim()
                    if (s.isEmpty() || s.equals("null", true) || s.equals("n/a", true)) null else s.toDouble()
                } catch (e: Exception) { null }
            })
            .registerTypeAdapter(Int::class.javaObjectType, JsonDeserializer { json, _, _ ->
                if (json == null || json.isJsonNull) null
                else try {
                    val s = json.asString.trim()
                    if (s.isEmpty() || s.equals("null", true) || s.equals("n/a", true)) null else s.toInt()
                } catch (e: Exception) { null }
            })
            .create()
    }

    val apiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(ApiConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(ApiService::class.java)
    }
}
