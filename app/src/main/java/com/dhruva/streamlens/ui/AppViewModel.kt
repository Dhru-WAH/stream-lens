package com.dhruva.streamlens.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dhruva.streamlens.StreamLensApp
import com.dhruva.streamlens.data.AlertPrefs
import com.dhruva.streamlens.data.Feed
import com.dhruva.streamlens.data.Filters
import com.dhruva.streamlens.data.Mark
import com.dhruva.streamlens.data.Release
import com.dhruva.streamlens.data.Sort
import com.dhruva.streamlens.data.Tab
import com.dhruva.streamlens.work.Alerts
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class Section(val label: String, val badge: String? = null, val items: List<Release>)
data class LangStat(val name: String, val watched: Int, val total: Int)

data class HomeState(
    val loaded: Boolean = false,
    val today: LocalDate = LocalDate.now(),
    val counts: Map<Tab, Int> = emptyMap(),
    val hero: List<Release> = emptyList(),
    val sections: List<Section> = emptyList(),
    val stats: List<LangStat> = emptyList(),
    val platforms: List<Pair<String, Int>> = emptyList(),
    val updatedAt: String? = null,
    val hiddenCount: Int = 0,
)

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as StreamLensApp
    private val dao = app.db.marks()

    val marks: StateFlow<Map<String, Mark>> = dao.all().map { list -> list.associateBy { it.id } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())
    val feed: StateFlow<Feed?> = app.feed.feed

    private val _filters = MutableStateFlow(app.prefs.filters)
    val filters: StateFlow<Filters> = _filters.asStateFlow()
    val query = MutableStateFlow("")
    val showHidden = MutableStateFlow(false)

    private val _alerts = MutableStateFlow(app.prefs.alerts)
    val alerts: StateFlow<AlertPrefs> = _alerts.asStateFlow()

    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()
    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages: SharedFlow<String> = _messages

    val home: StateFlow<HomeState> = combine(feed, marks, filters, query, showHidden) { f, m, fl, q, sh ->
        compute(f, m, fl, q, sh, LocalDate.now())
    }.stateIn(viewModelScope, SharingStarted.Eagerly, HomeState())

    init {
        viewModelScope.launch {
            app.feed.loadCached()
            refresh(quiet = true)
        }
    }

    fun refresh(quiet: Boolean = false) {
        if (_refreshing.value) return
        viewModelScope.launch {
            _refreshing.value = true
            app.feed.refresh()
                .onSuccess {
                    app.prefs.lastSync = System.currentTimeMillis()
                    Alerts.markSeen(app.prefs, it)
                    if (!quiet) _messages.tryEmit("Release list updated")
                }
                .onFailure { if (!quiet) _messages.tryEmit("Couldn't reach GitHub. Showing the last saved list.") }
            _refreshing.value = false
        }
    }

    fun updateFilters(change: (Filters) -> Filters) {
        val next = change(_filters.value)
        _filters.value = next
        app.prefs.filters = next
    }

    fun updateAlerts(change: (AlertPrefs) -> AlertPrefs) {
        val next = change(_alerts.value)
        _alerts.value = next
        app.prefs.alerts = next
    }

    private fun edit(id: String, change: (Mark) -> Mark) = viewModelScope.launch {
        val current = marks.value[id] ?: Mark(id)
        dao.upsert(change(current).copy(updatedAt = System.currentTimeMillis()))
    }

    fun toggleWatched(id: String) {
        val was = marks.value[id]?.watched == true
        edit(id) { if (was) it.copy(watched = false, watchedAt = null) else it.copy(watched = true, watchedAt = System.currentTimeMillis(), watchlist = false) }
        if (!was) _messages.tryEmit("Marked watched. Rate it from its details.")
    }

    fun toggleWatchlist(id: String) {
        val was = marks.value[id]?.watchlist == true
        edit(id) { it.copy(watchlist = !was) }
        _messages.tryEmit(if (was) "Removed from watchlist" else "Saved to watchlist")
    }

    fun rate(id: String, stars: Int) = edit(id) { it.copy(rating = if (it.rating == stars) 0 else stars) }

    fun toggleHidden(id: String) {
        val was = marks.value[id]?.hidden == true
        edit(id) { it.copy(hidden = !was) }
        if (!was) _messages.tryEmit("Hidden. Bring it back from Settings.")
    }

    /** "Pick for tonight": one of the best-rated unwatched titles that are streaming and match the filters. */
    fun pickForTonight(): Release? {
        val s = home.value
        val pool = s.sections.flatMap { it.items }.filter { it.isOut(s.today) && marks.value[it.id]?.watched != true }
        if (pool.isEmpty()) return null
        val ranked = pool.sortedWith(compareByDescending<Release> { it.score }.thenByDescending { it.verdictInfo?.rank ?: 0 })
        return ranked.take(6).random()
    }

    fun backup(uri: Uri) = viewModelScope.launch {
        runCatching { app.backup.write(uri) }
            .onSuccess { _messages.tryEmit("Backed up $it titles") }
            .onFailure { _messages.tryEmit("Backup failed: ${it.message}") }
    }

    fun restore(uri: Uri) = viewModelScope.launch {
        runCatching { app.backup.read(uri) }
            .onSuccess { _alerts.value = app.prefs.alerts; _messages.tryEmit("Restored $it titles") }
            .onFailure { _messages.tryEmit("Couldn't restore: ${it.message}") }
    }

    val lastSync: Long get() = app.prefs.lastSync
}

private val langRank = mapOf("te" to 0, "hi" to 1, "en" to 2)
private val byDateDesc = compareByDescending<Release> { it.ottDate ?: "" }.thenBy { langRank[it.lang] ?: 3 }.thenBy { it.title }
private val byDateAsc = compareBy<Release> { it.ottDate ?: "9999" }.thenBy { langRank[it.lang] ?: 3 }.thenBy { it.title }

private fun monthName(d: LocalDate) = d.month.getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.ENGLISH)

internal fun compute(feed: Feed?, marks: Map<String, Mark>, f: Filters, query: String, showHidden: Boolean, today: LocalDate): HomeState {
    if (feed == null) return HomeState()
    val all = feed.titles.filter { it.isVisible }
    val q = query.trim().lowercase()

    val passing = all.filter { r ->
        val m = marks[r.id]
        when {
            m?.hidden == true && !showHidden -> false
            f.lang != "all" && r.lang != f.lang -> false
            f.teluguAudio && !r.hasTeluguAudio -> false
            f.platforms.isNotEmpty() && r.platforms.none { it in f.platforms } -> false
            q.isNotEmpty() && listOf(r.title, r.cast, r.director, r.genres.joinToString(" "), r.platform).joinToString(" ").lowercase().contains(q).not() -> false
            else -> true
        }
    }

    fun inTab(r: Release, tab: Tab): Boolean {
        val m = marks[r.id]
        return when (tab) {
            Tab.STREAMING -> r.isOut(today) && !(f.unwatchedOnly && m?.watched == true)
            Tab.UPCOMING -> !r.isOut(today)
            Tab.WATCHLIST -> m?.watchlist == true && m.watched.not()
            Tab.WATCHED -> m?.watched == true
        }
    }

    val counts = Tab.entries.associateWith { t -> passing.count { inTab(it, t) } }
    val items = passing.filter { inTab(it, f.tab) }

    val hero = when (f.tab) {
        Tab.STREAMING -> items.filter { r -> r.ott?.let { !it.isBefore(today.minusDays(6)) } == true }
            .sortedWith(compareBy<Release> { langRank[it.lang] ?: 3 }.then(byDateDesc)).take(8)
        Tab.UPCOMING -> items.filter { r -> r.ott?.let { !it.isAfter(today.plusDays(7)) } == true }.sortedWith(byDateAsc).take(8)
        else -> emptyList()
    }

    val sections = LinkedHashMap<String, MutableList<Release>>()
    val badges = HashMap<String, String>()
    fun put(label: String, r: Release, badge: String? = null) {
        sections.getOrPut(label) { mutableListOf() }.add(r)
        if (badge != null) badges[label] = badge
    }

    if (f.sort != Sort.NEWEST && f.tab != Tab.UPCOMING) {
        val sorted = if (f.sort == Sort.TOP_RATED) items.sortedWith(compareByDescending<Release> { it.score }.then(byDateDesc))
        else items.sortedWith(compareByDescending<Release> { it.verdictInfo?.rank ?: -1 }.thenByDescending { it.score }.then(byDateDesc))
        for (r in sorted) {
            val has = if (f.sort == Sort.TOP_RATED) r.score >= 0 else r.verdictInfo != null
            put(
                when {
                    f.sort == Sort.TOP_RATED && has -> "Top rated"
                    f.sort == Sort.TOP_RATED -> "No rating yet"
                    has -> "Biggest box-office hits"
                    else -> "Direct to OTT or no verdict yet"
                },
                r,
            )
        }
    } else when (f.tab) {
        Tab.STREAMING -> for (r in items.sortedWith(byDateDesc)) {
            val d = r.ott
            put(
                when {
                    d == null -> "Streaming now"
                    !d.isBefore(today.minusDays(6)) -> "This week"
                    !d.isBefore(today.minusDays(13)) -> "Last week"
                    else -> "Earlier in ${monthName(d)}"
                },
                r,
            )
        }
        Tab.UPCOMING -> for (r in items.sortedWith(byDateAsc)) {
            val d = r.ott
            if (d == null) put("Date not announced", r)
            else {
                val days = java.time.temporal.ChronoUnit.DAYS.between(today, d)
                val label = "${d.dayOfWeek.getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.ENGLISH)}, ${d.dayOfMonth} ${d.month.getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.ENGLISH)}"
                put(label, r, if (days == 1L) "Tomorrow" else "In $days days")
            }
        }
        Tab.WATCHLIST -> {
            items.filter { it.isOut(today) }.sortedWith(byDateDesc).forEach { put("Ready to watch", it) }
            items.filterNot { it.isOut(today) }.sortedWith(byDateAsc).forEach { put("Waiting for release", it) }
        }
        Tab.WATCHED -> items.sortedByDescending { marks[it.id]?.watchedAt ?: 0L }.forEach { r ->
            val at = marks[r.id]?.watchedAt
            put(if (at == null) "Watched" else "Watched in " + monthName(java.time.Instant.ofEpochMilli(at).atZone(java.time.ZoneId.systemDefault()).toLocalDate()), r)
        }
    }

    val recent = all.filter { r -> r.isOut(today) && r.ott?.let { !it.isBefore(today.minusDays(30)) } == true && marks[r.id]?.hidden != true }
    val stats = Release.LANGS.mapNotNull { (code, name) ->
        val r = recent.filter { it.lang == code }
        if (r.isEmpty()) null else LangStat(name, r.count { marks[it.id]?.watched == true }, r.size)
    }

    val platformCounts = all.flatMap { it.platforms }.groupingBy { it }.eachCount().toList().sortedByDescending { it.second }

    return HomeState(
        loaded = true,
        today = today,
        counts = counts,
        hero = hero,
        sections = sections.map { (label, list) -> Section(label, badges[label], list) },
        stats = stats,
        platforms = platformCounts,
        updatedAt = feed.updatedAt,
        hiddenCount = all.count { marks[it.id]?.hidden == true },
    )
}
