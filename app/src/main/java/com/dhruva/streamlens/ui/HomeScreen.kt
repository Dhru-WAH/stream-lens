package com.dhruva.streamlens.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dhruva.streamlens.data.Filters
import com.dhruva.streamlens.data.Mark
import com.dhruva.streamlens.data.Sort
import com.dhruva.streamlens.data.Tab
import com.dhruva.streamlens.ui.theme.Display
import com.dhruva.streamlens.ui.theme.Palette
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val TAB_LABELS = listOf(Tab.STREAMING to "Streaming", Tab.UPCOMING to "Coming soon", Tab.WATCHLIST to "Watchlist", Tab.WATCHED to "Watched")
private val HERO = mapOf(
    Tab.STREAMING to Triple("Now", "streaming", "Fresh on OTT this week. Telugu first, then Hindi and English."),
    Tab.UPCOMING to Triple("Coming", "soon", "Announced OTT dates, nearest first. Bookmark one to remember it."),
    Tab.WATCHLIST to Triple("My", "watchlist", "Saved for later, ready ones first."),
    Tab.WATCHED to Triple("Already", "watched", "Your OTT diary, most recent first."),
)
private val EMPTY = mapOf(
    Tab.STREAMING to ("Nothing matches these filters" to "Try another language or platform, or turn off Unwatched only."),
    Tab.UPCOMING to ("No announced releases match" to "Try All languages or clear the platform filter."),
    Tab.WATCHLIST to ("Your watchlist is empty" to "Tap the bookmark on any upcoming poster, or Add to watchlist in a title's details."),
    Tab.WATCHED to ("Nothing marked yet" to "Tap the tick on a poster once you've seen it. You can rate it from its details."),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(vm: AppViewModel, onOpen: (String) -> Unit, onSettings: () -> Unit) {
    val home by vm.home.collectAsState()
    val marks by vm.marks.collectAsState()
    val filters by vm.filters.collectAsState()
    val query by vm.query.collectAsState()
    val refreshing by vm.refreshing.collectAsState()
    val grid = rememberLazyGridState()

    LaunchedEffect(filters.tab) { grid.scrollToItem(0) }

    Column(Modifier.fillMaxSize().background(Palette.Bg)) {
        TopBar(home, filters, onTab = { t -> vm.updateFilters { it.copy(tab = t) } }, onSettings = onSettings)
        PullToRefreshBox(isRefreshing = refreshing, onRefresh = { vm.refresh() }, modifier = Modifier.fillMaxSize()) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(150.dp),
                state = grid,
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 40.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                full("hero-head") {
                    HeroHeader(filters.tab, onPick = {
                        vm.pickForTonight()?.let { onOpen(it.id) }
                    })
                }
                if (home.hero.isNotEmpty()) full("hero") {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(home.hero, key = { it.id }) { r ->
                            Banner(
                                r, marks[r.id], home.today,
                                onOpen = { onOpen(r.id) },
                                onWatched = { vm.toggleWatched(r.id) },
                                onWatchlist = { vm.toggleWatchlist(r.id) },
                                modifier = Modifier.width(300.dp).height(310.dp),
                            )
                        }
                    }
                }
                if (filters.tab == Tab.STREAMING && home.stats.isNotEmpty()) full("stats") {
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        home.stats.forEach { StatRing(it) }
                    }
                }
                full("filters") { FilterDeck(filters, query, home.platforms, vm) }

                if (!home.loaded) full("loading") { EmptyState("Loading releases…", "One moment.") }
                else if (home.sections.isEmpty()) full("empty") {
                    val (h, b) = EMPTY.getValue(filters.tab)
                    EmptyState(h, b)
                }
                home.sections.forEach { section ->
                    full("h-${section.label}") { SectionHeader(section.label, section.badge, section.items.size, Modifier.padding(top = 4.dp)) }
                    posters(section.items.map { it.id to it }, marks, home, vm, onOpen)
                }
                full("foot") { Footer(home.updatedAt) }
            }
        }
    }
}

private fun LazyGridScope.full(key: String, content: @Composable () -> Unit) =
    item(key = key, span = { GridItemSpan(maxLineSpan) }) { content() }

private fun LazyGridScope.posters(
    items: List<Pair<String, com.dhruva.streamlens.data.Release>>,
    marks: Map<String, Mark>,
    home: HomeState,
    vm: AppViewModel,
    onOpen: (String) -> Unit,
) = items(items, key = { "p-${it.first}" }) { (id, r) ->
    PosterCard(
        r, marks[id], home.today,
        onOpen = { onOpen(id) },
        onQuick = { if (r.isOut(home.today)) vm.toggleWatched(id) else vm.toggleWatchlist(id) },
    )
}

@Composable
private fun TopBar(home: HomeState, filters: Filters, onTab: (Tab) -> Unit, onSettings: () -> Unit) {
    Column(Modifier.fillMaxWidth().background(Palette.Bg).padding(horizontal = 16.dp).padding(top = 8.dp, bottom = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                buildAnnotatedString {
                    append("STREAM")
                    withStyle(SpanStyle(color = Palette.Bulb)) { append("LENS") }
                },
                fontFamily = Display,
                fontWeight = FontWeight.Black,
                fontSize = 28.sp,
                color = Palette.Ink,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onSettings) { Icon(Icons.Rounded.Settings, "Settings", tint = Palette.Muted) }
        }
        Spacer(Modifier.height(8.dp))
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(999.dp)).background(Palette.Surface)
                .border(1.dp, Palette.Line, RoundedCornerShape(999.dp)).padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            TAB_LABELS.forEach { (tab, label) ->
                val selected = filters.tab == tab
                Surface(
                    onClick = { onTab(tab) },
                    shape = RoundedCornerShape(999.dp),
                    color = if (selected) Palette.Ink else Color.Transparent,
                    modifier = Modifier.weight(1f),
                ) {
                    Row(Modifier.padding(vertical = 8.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        Text(label, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = if (selected) Palette.Bg else Palette.Muted, maxLines = 1)
                        val n = home.counts[tab]
                        if (n != null && n > 0) Text(" $n", fontSize = 10.5.sp, color = if (selected) Palette.Bg.copy(alpha = 0.7f) else Palette.Dim)
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroHeader(tab: Tab, onPick: () -> Unit) {
    val (a, b, sub) = HERO.getValue(tab)
    Column(Modifier.padding(top = 14.dp)) {
        Text(
            buildAnnotatedString {
                append(a.uppercase() + " ")
                withStyle(SpanStyle(color = Palette.Bulb)) { append(b.uppercase()) }
            },
            style = MaterialTheme.typography.displaySmall,
        )
        Spacer(Modifier.height(8.dp))
        Bulbs()
        Spacer(Modifier.height(6.dp))
        Text(sub, style = MaterialTheme.typography.bodyMedium, color = Palette.Muted)
        if (tab == Tab.STREAMING) {
            Spacer(Modifier.height(12.dp))
            PillButton("Pick for tonight", Icons.Rounded.AutoAwesome, onPick, filled = true)
        }
    }
}

@Composable
private fun FilterDeck(filters: Filters, query: String, platforms: List<Pair<String, Int>>, vm: AppViewModel) {
    Column(Modifier.padding(top = 6.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Segmented(
                listOf("all" to "All", "te" to "Telugu", "hi" to "Hindi", "en" to "English"),
                filters.lang,
            ) { v -> vm.updateFilters { it.copy(lang = v) } }
            if (filters.tab != Tab.UPCOMING) Segmented(
                listOf(Sort.NEWEST to "Newest", Sort.TOP_RATED to "Top rated", Sort.BIGGEST_HITS to "Biggest hits"),
                filters.sort,
            ) { v -> vm.updateFilters { it.copy(sort = v) } }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Toggle("Telugu audio", filters.teluguAudio) { v -> vm.updateFilters { it.copy(teluguAudio = v) } }
            if (filters.tab == Tab.STREAMING) Toggle("Unwatched only", filters.unwatchedOnly) { v -> vm.updateFilters { it.copy(unwatchedOnly = v) } }
        }
        OutlinedTextField(
            value = query,
            onValueChange = { vm.query.value = it },
            placeholder = { Text("Search a title, actor or director", color = Palette.Dim) },
            leadingIcon = { Icon(Icons.Rounded.Search, null, tint = Palette.Dim) },
            trailingIcon = if (query.isNotEmpty()) ({ IconButton(onClick = { vm.query.value = "" }) { Icon(Icons.Rounded.Close, "Clear search", tint = Palette.Muted) } }) else null,
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Palette.Surface, unfocusedContainerColor = Palette.Surface,
                focusedBorderColor = Palette.Bulb, unfocusedBorderColor = Palette.Line,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                PlatformTile("All platforms", null, filters.platforms.isEmpty(), Palette.Surface2) { vm.updateFilters { it.copy(platforms = emptySet()) } }
            }
            items(platforms, key = { it.first }) { (p, n) ->
                PlatformTile(p, n, p in filters.platforms, Palette.platform(p)) {
                    vm.updateFilters { f -> f.copy(platforms = if (p in f.platforms) f.platforms - p else f.platforms + p) }
                }
            }
        }
    }
}

@Composable
private fun <T> Segmented(options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit) {
    Row(
        Modifier.clip(RoundedCornerShape(12.dp)).background(Palette.Surface).border(1.dp, Palette.Line, RoundedCornerShape(12.dp)).padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        options.forEach { (value, label) ->
            val on = value == selected
            Surface(onClick = { onSelect(value) }, shape = RoundedCornerShape(8.dp), color = if (on) Palette.Surface2 else Color.Transparent) {
                Text(
                    label,
                    color = if (on) Palette.Ink else Palette.Muted,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.5.sp,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun Toggle(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Palette.BulbInk, checkedTrackColor = Palette.Bulb,
                uncheckedThumbColor = Palette.Muted, uncheckedTrackColor = Palette.Surface2, uncheckedBorderColor = Palette.Line,
            ),
            modifier = Modifier.size(width = 44.dp, height = 26.dp),
        )
        Text(label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = if (checked) Palette.Ink else Palette.Muted)
    }
}

@Composable
private fun PlatformTile(name: String, count: Int?, selected: Boolean, color: Color, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = color.copy(alpha = if (selected) 1f else 0.55f),
        border = if (selected) androidx.compose.foundation.BorderStroke(2.dp, Palette.Ink) else null,
    ) {
        Row(Modifier.padding(horizontal = 13.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(name, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 13.5.sp)
            if (count != null) Text(
                "$count",
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(Color.Black.copy(alpha = 0.28f)).padding(horizontal = 6.dp, vertical = 1.dp),
            )
        }
    }
}

@Composable
private fun Footer(updatedAt: String?) {
    val text = updatedAt?.let {
        runCatching { OffsetDateTime.parse(it).format(DateTimeFormatter.ofPattern("d MMM, h:mm a", Locale.ENGLISH)) }.getOrNull()
    }
    Text(
        if (text != null) "Release list updated $text · pull down to refresh" else "Pull down to refresh",
        style = MaterialTheme.typography.bodySmall,
        color = Palette.Dim,
        modifier = Modifier.padding(top = 18.dp),
    )
}
