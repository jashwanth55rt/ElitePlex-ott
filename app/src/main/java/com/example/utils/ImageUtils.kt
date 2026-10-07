package com.example.utils

object ImageUtils {

    private const val API_ORIGIN = "https://eliteplex-api.vercel.app"

    /**
     * Resolves and normalizes any image path or URL from various API responses
     * into a valid, secure HTTPS URL.
     * Supported fields in order of priority:
     * poster, poster_url, posterUrl, poster_path, posterPath,
     * image, image_url, imageUrl, thumbnail, thumbnail_url,
     * backdrop, backdrop_url, backdrop_path
     */
    fun normalizeImageUrl(url: String?): String? {
        if (url.isNullOrBlank()) return null
        var trimmed = url.trim()

        if (trimmed.isEmpty() || trimmed.equals("null", ignoreCase = true)) {
            return null
        }

        // If it starts with "//" (protocol relative)
        if (trimmed.startsWith("//")) {
            return "https:$trimmed"
        }

        // If it begins with a relative path '/', prepend the API origin (never twice)
        if (trimmed.startsWith("/")) {
            return "$API_ORIGIN$trimmed"
        }

        // If it returns HTTP, convert to HTTPS
        if (trimmed.startsWith("http://", ignoreCase = true)) {
            trimmed = "https://" + trimmed.substring(7)
        }

        return trimmed
    }

    /**
     * Finds the best available image URL among potential candidates, prioritizing
     * poster -> image/thumbnail -> backdrop.
     */
    fun getBestImageUrl(vararg candidates: String?): String? {
        for (candidate in candidates) {
            val normalized = normalizeImageUrl(candidate)
            if (!normalized.isNullOrBlank()) {
                return normalized
            }
        }
        return null
    }
}
