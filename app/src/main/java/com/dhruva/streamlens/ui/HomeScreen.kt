package com.dhruva.streamlens.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.LiveTv
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dhruva.streamlens.data.Filters
import com.dhruva.streamlens.data.Mark
import com.dhruva.streamlens.data.Release
import com.dhruva.streamlens.data.Sort
import com.dhruva.streamlens.data.Tab
import com.dhruva.streamlens.ui.theme.Body
import com.dhruva.streamlens.ui.theme.Display
import com.dhruva.streamlens.ui.theme.Palette
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private data class TabInfo(val tab: Tab, val label: String, val icon: ImageVector, val head: String, val accent: String)

private val TABS = listOf(
    TabInfo(Tab.STREAMING, "Streaming", Icons.Rounded.LiveTv, "Now", "streaming"),
    TabInfo(Tab.UPCOMING, "Upcoming", Icons.Rounded.CalendarMonth, "Upcoming", "releases"),
    TabInfo(Tab.WATCHLIST, "Watchlist", Icons.Rounded.Bookmark, "My", "watchlist"),
    TabInfo(Tab.WATCHED, "Watched", Icons.Rounded.TaskAlt, "Already", "watched"),
)
private val SORTS = listOf(Sort.NEWEST to "Newest", Sort.TOP_RATED to "Top rated", Sort.BIGGEST_HITS to "Biggest hits")
private val EMPTY = mapOf(
    Tab.STREAMING to ("Nothing matches these filters" to "Try another language, or clear the filters."),
    Tab.UPCOMING to ("No announced releases match" to "Try All languages or clear the filters."),
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
    var showFilters by remember { mutableStateOf(false) }

    LaunchedEffect(filters.tab) { grid.scrollToItem(0) }

    Column(Modifier.fillMaxSize().background(Palette.Bg)) {
        Header(
            showPick = filters.tab == Tab.STREAMING,
            onPick = { vm.pickForTonight()?.let { onOpen(it.id) } },
            onSettings = onSettings,
        )
        PullToRefreshBox(isRefreshing = refreshing, onRefresh = { vm.refresh() }, modifier = Modifier.weight(1f)) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(150.dp),
                state = grid,
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 28.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                full("heading") { Heading(TABS.first { it.tab == filters.tab }, home.counts[filters.tab] ?: 0) }
                if (filters.tab == Tab.WATCHED && home.stats.isNotEmpty()) full("stats") {
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        home.stats.forEach { StatRing(it) }
                    }
                }
                full("controls") {
                    Controls(filters, query, vm, onOpenFilters = { showFilters = true })
                }

                if (!home.loaded) full("loading") { EmptyState("Loading releases…", "One moment.") }
                else if (home.sections.isEmpty()) full("empty") {
                    val (h, b) = EMPTY.getValue(filters.tab)
                    EmptyState(h, b)
                }
                home.sections.forEach { section ->
                    full("h-${section.label}") { SectionHeader(section.label, section.badge, section.items.size) }
                    posters(section.items, marks, home, vm, onOpen)
                }
                full("foot") { Footer(home.updatedAt) }
            }
        }
        BottomNav(filters.tab, home.counts) { t -> vm.updateFilters { it.copy(tab = t) } }
    }

    if (showFilters) FilterSheet(filters, home.platforms, vm) { showFilters = false }
}

private fun LazyGridScope.full(key: String, content: @Composable () -> Unit) =
    item(key = key, span = { GridItemSpan(maxLineSpan) }) { content() }

private fun LazyGridScope.posters(items: List<Release>, marks: Map<String, Mark>, home: HomeState, vm: AppViewModel, onOpen: (String) -> Unit) =
    items(items, key = { "p-${it.id}" }) { r ->
        PosterCard(
            r, marks[r.id], home.today,
            onOpen = { onOpen(r.id) },
            onQuick = { if (r.isOut(home.today)) vm.toggleWatched(r.id) else vm.toggleWatchlist(r.id) },
        )
    }

/** Wordmark in the house ivory and gold, with the signature beneath it. */
@Composable
private fun Header(showPick: Boolean, onPick: () -> Unit, onSettings: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 6.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(
                buildAnnotatedString {
                    append("Stream")
                    withStyle(SpanStyle(color = Palette.GoldOnDark)) { append("Lens") }
                },
                fontFamily = Body,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 24.sp,
                letterSpacing = (-0.3).sp,
                color = Palette.Ivory,
            )
            Signature(size = 15.sp, onDark = true, modifier = Modifier.padding(start = 2.dp))
        }
        if (showPick) IconButton(onClick = onPick) { Icon(Icons.Rounded.AutoAwesome, "Pick something for tonight", tint = Palette.GoldOnDark) }
        IconButton(onClick = onSettings) { Icon(Icons.Rounded.Settings, "Settings", tint = Palette.Muted) }
    }
}

@Composable
private fun Heading(info: TabInfo, count: Int) {
    Column(Modifier.padding(top = 10.dp)) {
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                buildAnnotatedString {
                    append(info.head.uppercase() + " ")
                    withStyle(SpanStyle(color = Palette.Bulb)) { append(info.accent.uppercase()) }
                },
                style = MaterialTheme.typography.displaySmall.copy(fontSize = 34.sp, lineHeight = 34.sp),
            )
            if (count > 0) Text("$count", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(bottom = 5.dp))
        }
        Spacer(Modifier.height(6.dp))
        Bulbs(count = 14)
    }
}

/** Search + Filters on one line, then language and sort as one scrollable row of chips. */
@Composable
private fun Controls(filters: Filters, query: String, vm: AppViewModel, onOpenFilters: () -> Unit) {
    val activeFilters = (if (filters.teluguAudio) 1 else 0) + filters.platforms.size
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SearchField(query, onChange = { vm.query.value = it }, modifier = Modifier.weight(1f))
            Surface(
                onClick = onOpenFilters,
                shape = RoundedCornerShape(12.dp),
                color = if (activeFilters > 0) Palette.Bulb.copy(alpha = 0.14f) else Palette.Surface,
                border = BorderStroke(1.dp, if (activeFilters > 0) Palette.Bulb.copy(alpha = 0.6f) else Palette.Line),
                modifier = Modifier.height(44.dp),
            ) {
                Row(Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    BadgedBox(badge = { if (activeFilters > 0) Badge(containerColor = Palette.Bulb, contentColor = Palette.BulbInk) { Text("$activeFilters") } }) {
                        Icon(Icons.Rounded.Tune, null, tint = if (activeFilters > 0) Palette.Bulb else Palette.Ink, modifier = Modifier.size(20.dp))
                    }
                    Text("Filters", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            listOf("all" to "All", "te" to "Telugu", "hi" to "Hindi", "en" to "English").forEach { (code, label) ->
                Chip(label, filters.lang == code) { vm.updateFilters { it.copy(lang = code) } }
            }
            if (filters.tab != Tab.UPCOMING) {
                Box(Modifier.padding(horizontal = 4.dp).width(1.dp).height(20.dp).background(Palette.Line))
                SortChip(filters.sort) { s -> vm.updateFilters { it.copy(sort = s) } }
            }
        }
    }
}

@Composable
private fun SearchField(query: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.height(44.dp).clip(RoundedCornerShape(12.dp)).background(Palette.Surface).padding(start = 12.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.Search, null, tint = Palette.Dim, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Box(Modifier.weight(1f)) {
            if (query.isEmpty()) Text("Search title, actor, director", color = Palette.Dim, fontSize = 14.5.sp, maxLines = 1)
            BasicTextField(
                value = query,
                onValueChange = onChange,
                singleLine = true,
                textStyle = TextStyle(fontFamily = Body, fontSize = 14.5.sp, color = Palette.Ink),
                cursorBrush = SolidColor(Palette.Bulb),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (query.isNotEmpty()) IconButton(onClick = { onChange("") }, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Rounded.Close, "Clear search", tint = Palette.Muted, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun Chip(label: String, selected: Boolean, trailing: ImageVector? = null, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(999.dp),
        color = if (selected) Palette.Ink else Color.Transparent,
        border = BorderStroke(1.dp, if (selected) Palette.Ink else Palette.Line),
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = if (selected) Palette.Bg else Palette.Muted)
            if (trailing != null) Icon(trailing, null, tint = if (selected) Palette.Bg else Palette.Muted, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun SortChip(sort: Sort, onSelect: (Sort) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        Chip(SORTS.first { it.first == sort }.second, selected = false, trailing = Icons.Rounded.ExpandMore) { open = true }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }, containerColor = Palette.Surface2) {
            SORTS.forEach { (s, label) ->
                DropdownMenuItem(
                    text = { Text(label, fontWeight = if (s == sort) FontWeight.ExtraBold else FontWeight.Normal, color = if (s == sort) Palette.Bulb else Palette.Ink) },
                    onClick = { onSelect(s); open = false },
                )
            }
        }
    }
}

@Composable
private fun BottomNav(selected: Tab, counts: Map<Tab, Int>, onSelect: (Tab) -> Unit) {
    Column {
        HorizontalDivider(color = Palette.Line, thickness = 1.dp)
        NavigationBar(containerColor = Palette.Bg2, tonalElevation = 0.dp, windowInsets = WindowInsets(0)) {
            TABS.forEach { info ->
                val on = info.tab == selected
                NavigationBarItem(
                    selected = on,
                    onClick = { onSelect(info.tab) },
                    icon = {
                        val n = counts[info.tab] ?: 0
                        BadgedBox(badge = { if (info.tab == Tab.WATCHLIST && n > 0) Badge(containerColor = Palette.Bulb, contentColor = Palette.BulbInk) { Text("$n") } }) {
                            Icon(info.icon, null)
                        }
                    },
                    label = { Text(info.label, fontSize = 12.sp, fontWeight = if (on) FontWeight.ExtraBold else FontWeight.SemiBold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Palette.BulbInk,
                        selectedTextColor = Palette.Ink,
                        indicatorColor = Palette.Bulb,
                        unselectedIconColor = Palette.Muted,
                        unselectedTextColor = Palette.Muted,
                    ),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterSheet(filters: Filters, platforms: List<Pair<String, Int>>, vm: AppViewModel, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = Palette.Bg2) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("FILTERS", fontFamily = Display, fontWeight = FontWeight.Black, fontSize = 26.sp)
            SwitchRow("Has Telugu audio", "Telugu films plus Hindi or English films dubbed in Telugu", filters.teluguAudio) { v ->
                vm.updateFilters { it.copy(teluguAudio = v) }
            }
            SwitchRow("Unwatched only", "Hide what you've already ticked on the Streaming tab", filters.unwatchedOnly) { v ->
                vm.updateFilters { it.copy(unwatchedOnly = v) }
            }
            Text("PLATFORMS", style = MaterialTheme.typography.labelSmall, color = Palette.Bulb)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                platforms.forEach { (p, n) ->
                    PlatformTile(p, n, p in filters.platforms, Palette.platform(p)) {
                        vm.updateFilters { f -> f.copy(platforms = if (p in f.platforms) f.platforms - p else f.platforms + p) }
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = { vm.updateFilters { it.copy(teluguAudio = false, platforms = emptySet()) } }) {
                    Text("Clear filters", color = Palette.Muted)
                }
                PillButton("Done", null, onDismiss, filled = true)
            }
        }
    }
}

@Composable
private fun SwitchRow(label: String, hint: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(label, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(hint, style = MaterialTheme.typography.bodySmall)
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Palette.BulbInk, checkedTrackColor = Palette.Bulb,
                uncheckedThumbColor = Palette.Muted, uncheckedTrackColor = Palette.Surface2, uncheckedBorderColor = Palette.Line,
            ),
        )
    }
}

@Composable
private fun PlatformTile(name: String, count: Int, selected: Boolean, color: Color, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = color.copy(alpha = if (selected) 1f else 0.5f),
        border = if (selected) BorderStroke(2.dp, Palette.Ink) else null,
    ) {
        Row(Modifier.padding(horizontal = 13.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(name, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 13.5.sp)
            Text(
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
