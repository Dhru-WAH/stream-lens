package com.dhruva.streamlens.data

import android.content.Context
import com.dhruva.streamlens.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.CacheControl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * The release list: downloaded from GitHub, cached in app storage for offline use, and seeded from the
 * copy bundled in assets so the first launch shows something even without a network.
 */
class FeedRepository(private val context: Context) {

    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val cacheFile = File(context.filesDir, "releases.json")
    private val _feed = MutableStateFlow<Feed?>(null)
    val feed: StateFlow<Feed?> = _feed.asStateFlow()

    suspend fun loadCached() = withContext(Dispatchers.IO) {
        if (_feed.value != null) return@withContext
        val text = if (cacheFile.exists()) cacheFile.readText()
        else context.assets.open("releases.json").bufferedReader().use { it.readText() }
        _feed.value = runCatching { FeedJson.decodeFromString<Feed>(text) }.getOrNull()
    }

    /** Fetches the latest list. On failure the cached list stays in place and the error is returned. */
    suspend fun refresh(): Result<Feed> = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url(BuildConfig.FEED_URL)
                .cacheControl(CacheControl.FORCE_NETWORK)
                .build()
            val text = http.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
                response.body.string()
            }
            val feed = FeedJson.decodeFromString<Feed>(text)
            require(feed.titles.isNotEmpty()) { "Empty release list" }
            val tmp = File(cacheFile.parentFile, "releases.json.tmp")
            tmp.writeText(text)
            tmp.renameTo(cacheFile)
            _feed.value = feed
            feed
        }
    }
}
