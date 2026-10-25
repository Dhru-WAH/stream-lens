package com.dhruva.streamlens.data

import android.content.Context
import androidx.core.content.edit
import kotlinx.serialization.Serializable

enum class Tab { STREAMING, UPCOMING, WATCHLIST, WATCHED }
enum class Sort { NEWEST, TOP_RATED, BIGGEST_HITS }

/** Filter and sort choices, remembered between launches. */
@Serializable
data class Filters(
    val tab: Tab = Tab.STREAMING,
    val lang: String = "all",
    val teluguAudio: Boolean = false,
    val unwatchedOnly: Boolean = true,
    val sort: Sort = Sort.NEWEST,
    val platforms: Set<String> = emptySet(),
)

@Serializable
data class AlertPrefs(
    val watchlistStreaming: Boolean = true,
    val newTelugu: Boolean = true,
    val newHindi: Boolean = false,
    val newEnglish: Boolean = false,
) {
    fun wantsNew(lang: String) = when (lang) {
        "te" -> newTelugu
        "hi" -> newHindi
        "en" -> newEnglish
        else -> false
    }
}

class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("streamlens", Context.MODE_PRIVATE)

    var filters: Filters
        get() = sp.getString("filters", null)?.let { runCatching { FeedJson.decodeFromString<Filters>(it) }.getOrNull() } ?: Filters()
        set(value) = sp.edit { putString("filters", FeedJson.encodeToString(Filters.serializer(), value)) }

    var alerts: AlertPrefs
        get() = sp.getString("alerts", null)?.let { runCatching { FeedJson.decodeFromString<AlertPrefs>(it) }.getOrNull() } ?: AlertPrefs()
        set(value) = sp.edit { putString("alerts", FeedJson.encodeToString(AlertPrefs.serializer(), value)) }

    /** Every id I've already seen, so new arrivals can be told apart. Empty until the first sync. */
    var knownIds: Set<String>
        get() = sp.getStringSet("knownIds", emptySet()).orEmpty()
        set(value) = sp.edit { putStringSet("knownIds", value) }

    /** Watchlist titles I've already been told are streaming. */
    var notifiedIds: Set<String>
        get() = sp.getStringSet("notifiedIds", emptySet()).orEmpty()
        set(value) = sp.edit { putStringSet("notifiedIds", value) }

    var lastSync: Long
        get() = sp.getLong("lastSync", 0L)
        set(value) = sp.edit { putLong("lastSync", value) }

    var askedNotificationPermission: Boolean
        get() = sp.getBoolean("askedNotif", false)
        set(value) = sp.edit { putBoolean("askedNotif", value) }
}
