package com.example.data.model

import com.google.gson.annotations.SerializedName

data class PlayResponse(
    @SerializedName("creator")
    val creator: String? = null,
    @SerializedName("tmdb_id")
    val tmdbId: String? = null,
    @SerializedName("media")
    val media: String? = null,
    @SerializedName("title")
    val title: String? = null,
    @SerializedName("se")
    val season: Int? = null,
    @SerializedName("ep")
    val episode: Int? = null,
    @SerializedName("count")
    val count: Int? = null,
    @SerializedName("sources")
    val sources: List<PlaySource>? = null
)

data class PlaySource(
    @SerializedName("provider")
    val provider: String? = null,
    @SerializedName("url")
    val url: String? = null,
    @SerializedName("play_url")
    val playUrl: String? = null,
    @SerializedName("type")
    val type: String? = null,
    @SerializedName("format")
    val format: String? = null,
    @SerializedName("label")
    val label: String? = null,
    @SerializedName("phone_friendly")
    val phoneFriendly: Boolean? = null
) {
    val bestUrl: String
        get() {
            val raw = playUrl?.takeIf { it.isNotBlank() } ?: url ?: ""
            return when {
                raw.startsWith("http://") || raw.startsWith("https://") -> raw
                raw.startsWith("/") -> "https://eliteplex-api.vercel.app$raw"
                raw.isNotBlank() -> "https://eliteplex-api.vercel.app/$raw"
                else -> ""
            }
        }

    val displayLabel: String
        get() = label?.takeIf { it.isNotBlank() }
            ?: provider?.takeIf { it.isNotBlank() }
            ?: "Server"

    val isEmbed: Boolean
        get() = type.equals("embed", ignoreCase = true) || format.equals("EMBED", ignoreCase = true)

    val isDirectStream: Boolean
        get() {
            val u = bestUrl.lowercase()
            return format.equals("DASH", ignoreCase = true) ||
                    format.equals("HLS", ignoreCase = true) ||
                    format.equals("MP4", ignoreCase = true) ||
                    u.endsWith(".mp4") || u.endsWith(".m3u8") || u.endsWith(".mpd") ||
                    u.contains(".mpd?") || u.contains(".m3u8?") ||
                    u.contains("/mb/proxy/mpd")
        }
}
