package com.dhruva.streamlens.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dhruva.streamlens.data.Release
import com.dhruva.streamlens.data.Tier
import com.dhruva.streamlens.ui.theme.Display
import com.dhruva.streamlens.ui.theme.Palette
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailSheet(id: String, vm: AppViewModel, onDismiss: () -> Unit) {
    val feed by vm.feed.collectAsState()
    val marks by vm.marks.collectAsState()
    val r = feed?.titles?.firstOrNull { it.id == id } ?: return
    val mark = marks[id]
    val today = LocalDate.now()
    val out = r.isOut(today)
    val context = LocalContext.current
    fun open(url: String) = runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Palette.Bg2,
        dragHandle = null,
    ) {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            Box(Modifier.fillMaxWidth().aspectRatio(16f / 10f)) {
                PosterArt(r, Modifier.fillMaxSize())
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0f to Color.Black.copy(alpha = 0.25f), 0.3f to Color.Transparent, 0.98f to Palette.Bg2)))
                Column(Modifier.align(Alignment.BottomStart).padding(horizontal = 20.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        PlatformBadge(r.primaryPlatform, 12.sp)
                        Tag(r.langName, fontSize = 12.sp)
                        if (r.isSeries) Tag("Series", fontSize = 12.sp)
                    }
                    Text(r.title.uppercase(), fontFamily = Display, fontWeight = FontWeight.Black, fontSize = 40.sp, lineHeight = 38.sp, color = Color.White)
                    Text(whenText(r, today).uppercase(), color = Palette.Bulb, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.8.sp)
                }
            }

            Column(Modifier.padding(horizontal = 20.dp).padding(top = 6.dp, bottom = 36.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (out) PillButton(
                        if (mark?.watched == true) "Watched" else "Mark as watched", Icons.Rounded.Check, { vm.toggleWatched(id) },
                        filled = true, fill = if (mark?.watched == true) Palette.Good else Palette.Bulb,
                        ink = if (mark?.watched == true) Palette.GoodInk else Palette.BulbInk, modifier = Modifier.weight(1f),
                    )
                    if (mark?.watched != true) PillButton(
                        when {
                            mark?.watchlist == true -> "On watchlist"
                            out -> "Add to watchlist"
                            else -> "Remind me"
                        },
                        if (mark?.watchlist == true) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                        { vm.toggleWatchlist(id) },
                        modifier = Modifier.weight(1f),
                    )
                }
                if (mark?.watched == true) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Your rating", color = Palette.Muted, fontSize = 14.sp)
                    (1..5).forEach { n ->
                        TextButton(onClick = { vm.rate(id, n) }, contentPadding = androidx.compose.foundation.layout.PaddingValues(2.dp), modifier = Modifier.width(36.dp)) {
                            Text("★", fontSize = 26.sp, color = if ((mark.rating) >= n) Palette.Bulb else Palette.Surface2)
                        }
                    }
                }
                if (r.genres.isNotEmpty() || r.isExpected) FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    r.genres.forEach { Chip(it) }
                    if (r.isExpected) Chip("Date not officially confirmed", Palette.Bulb, Palette.Bulb.copy(alpha = 0.15f))
                }
                r.synopsis?.let { Text(it, fontSize = 15.5.sp, lineHeight = 23.sp, color = Color(0xFFE6DCEA)) }

                Performance(r, out, ::open)
                BoxOffice(r, ::open)

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Detail("On OTT", r.ott?.let { "${it.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH)}, ${it.dayOfMonth} ${it.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH)}" + if (r.isExpected) " (expected)" else "" } ?: "Not announced")
                    Detail("Platform", r.platform ?: "TBA")
                    r.theatrical?.let { Detail("In theatres", "${it.dayOfMonth} ${it.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH)} ${it.year}") }
                    r.director?.let { Detail("Director", it) }
                    r.cast?.let { Detail("Cast", it) }
                    val dubs = r.dubs.filter { it != r.lang && it in Release.LANGS }.map { Release.LANGS.getValue(it) }
                    if (dubs.isNotEmpty()) Detail("Also in", dubs.joinToString(", "))
                }

                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val lang = when (r.lang) { "te" -> " telugu"; "hi" -> " hindi"; else -> "" }
                    LinkButton("Trailer", Icons.Rounded.PlayArrow) { open("https://www.youtube.com/results?search_query=" + Uri.encode("${r.title} ${r.year ?: ""}$lang trailer")) }
                    LinkButton("Where to watch", Icons.AutoMirrored.Rounded.OpenInNew) { open("https://www.justwatch.com/in/search?q=" + Uri.encode(r.title)) }
                    r.source?.let { src -> LinkButton("Source", Icons.AutoMirrored.Rounded.OpenInNew) { open(src) } }
                }
                TextButton(onClick = { vm.toggleHidden(id); if (mark?.hidden != true) onDismiss() }) {
                    Text(if (mark?.hidden == true) "Show this title again" else "Not interested, hide it", color = Palette.Muted)
                }
            }
        }
    }
}

@Composable
private fun Chip(text: String, ink: Color = Palette.Ink, bg: Color = Palette.Surface2) {
    Text(text, color = ink, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(bg).padding(horizontal = 9.dp, vertical = 3.dp))
}

@Composable
private fun Detail(label: String, value: String) {
    Row {
        Text(label, color = Palette.Muted, fontSize = 14.sp, modifier = Modifier.width(110.dp))
        Text(value, fontSize = 14.sp, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun LinkButton(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = RoundedCornerShape(10.dp), color = Palette.Surface, border = androidx.compose.foundation.BorderStroke(1.dp, Palette.Line)) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(icon, null, tint = Palette.Ink, modifier = Modifier.padding(0.dp).width(16.dp))
            Text(text, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun Tile(label: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(
        modifier.clip(RoundedCornerShape(12.dp)).background(Palette.Surface).border(1.dp, Palette.Line, RoundedCornerShape(12.dp)).padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = Palette.Muted)
        content()
    }
}

@Composable
private fun Score(value: Double, sub: String?, onSub: (() -> Unit)?) {
    Text(
        androidx.compose.ui.text.buildAnnotatedString {
            append(String.format(Locale.US, "%.1f", value))
            withStyle(androidx.compose.ui.text.SpanStyle(fontSize = 13.sp, color = Palette.Muted)) { append("/10") }
        },
        fontSize = 22.sp,
        fontWeight = FontWeight.SemiBold,
    )
    if (sub != null) Text(sub, style = MaterialTheme.typography.bodySmall, modifier = if (onSub != null) Modifier.then(Modifier.padding(0.dp)) else Modifier)
}

/** IMDb, BookMyShow and critics, side by side. */
@Composable
private fun Performance(r: Release, out: Boolean, open: (String) -> Unit) {
    val rv = mapOf("Positive" to "Good reviews", "Mixed" to "Mixed reviews", "Negative" to "Poor reviews")[r.reception]
    if (r.imdb == null && r.bms == null && r.criticScore == null && rv == null) {
        if (!out) Text("Not out yet, so no ratings or verdict.", style = MaterialTheme.typography.bodySmall)
        return
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        r.imdb?.let { Tile("IMDb", Modifier.weight(1f)) { Score(it, r.imdbVotes?.let { v -> "$v votes" }, null) } }
        r.bms?.let { Tile("BookMyShow", Modifier.weight(1f)) { Score(it, r.bmsVotes?.let { v -> "$v votes" }, null) } }
        if (r.criticScore != null || rv != null) Tile("Critics", Modifier.weight(1f)) {
            r.criticScore?.let { Text(it, fontSize = 15.sp, fontWeight = FontWeight.SemiBold) }
            if (rv != null) Text(
                rv,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = when (r.reception) { "Positive" -> Palette.Good; "Negative" -> Palette.Rose; else -> Palette.Muted },
            )
        }
    }
}

private fun money(n: Double, usd: Boolean): String = if (usd) {
    if (n >= 1000) "$" + String.format(Locale.US, "%.2f", n / 1000).trimEnd('0').trimEnd('.') + "B" else "$" + String.format(Locale.US, "%.1f", n).removeSuffix(".0") + "M"
} else "₹" + NumberFormat.getIntegerInstance(Locale("en", "IN")).format(Math.round(n)) + " cr"

/** Verdict, budget vs worldwide gross as a bar, and the break-even line where it's a gross figure. */
@Composable
private fun BoxOffice(r: Release, open: (String) -> Unit) {
    val v = r.verdictInfo
    val b = r.budgetAmt
    val g = r.grossAmt
    val beAny = r.breakEvenAmt
    if (v == null && b == null && g == null && r.boxOffice == null) return
    val usd = r.money == "USD"
    val be = if (r.breakEvenKind == "gross") beAny else null

    Tile("Box office", Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) { if (v != null) VerdictChip(r, LocalDate.now(), 12.sp) }
            if (b != null && b > 0 && g != null) Text(
                androidx.compose.ui.text.buildAnnotatedString {
                    append(String.format(Locale.US, "%.1f×", g / b))
                    withStyle(androidx.compose.ui.text.SpanStyle(fontSize = 12.5.sp, color = Palette.Muted)) { append(" budget") }
                },
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        if (b != null && b > 0 && g != null) {
            val max = maxOf(g, b, be ?: 0.0) * 1.08
            val fill = when (v?.tier) {
                Tier.TOP -> Palette.Bulb
                Tier.HIT -> Color(0xFF2FB673)
                Tier.AVG -> Color(0xFF8A7DA3)
                Tier.LOW -> Color(0xFFE86A43)
                Tier.BAD -> Color(0xFFC0183A)
                null -> Palette.Good
            }
            Spacer(Modifier.height(14.dp))
            BoxWithConstraints(Modifier.fillMaxWidth().height(12.dp)) {
                val w = maxWidth
                Box(Modifier.fillMaxSize().clip(RoundedCornerShape(6.dp)).background(Palette.Surface2))
                Box(Modifier.fillMaxWidth((g / max).toFloat().coerceIn(0f, 1f)).height(12.dp).clip(RoundedCornerShape(6.dp)).background(fill))
                Marker(w * (b / max).toFloat(), Palette.Ink)
                if (be != null) Marker(w * (be / max).toFloat(), Palette.Bulb)
            }
            Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("▏Budget", fontSize = 11.sp, color = Palette.Muted)
                if (be != null) Text("▏Break-even", fontSize = 11.sp, color = Palette.Bulb)
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (b != null) Figure("Budget", money(b, usd), Modifier.weight(1f))
            if (g != null) Figure("Worldwide gross", money(g, usd), Modifier.weight(1f))
            if (beAny != null) Figure(if (be != null) "Break-even (gross)" else "Break-even (share)", money(beAny, usd), Modifier.weight(1f))
        }
        val notes = listOfNotNull(
            if (beAny != null && be == null) "Share is the producers' and distributors' cut of ticket sales, roughly half of gross." else null,
            r.budgetNote,
            if (g == null) r.boxOffice else null,
            if (b == null && (v != null || g != null)) "Budget not reported." else null,
        )
        notes.forEach { Text(it, style = MaterialTheme.typography.bodySmall) }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            r.verdictSource?.let { TextButton(onClick = { open(it) }, contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) { Text("Verdict source", fontSize = 12.5.sp, color = Palette.Muted) } }
            r.budgetSource?.let { TextButton(onClick = { open(it) }, contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) { Text("Budget source", fontSize = 12.5.sp, color = Palette.Muted) } }
        }
    }
}

@Composable
private fun Marker(x: androidx.compose.ui.unit.Dp, color: Color) {
    Box(Modifier.offset(x = x - 1.dp, y = (-4).dp).width(2.dp).height(20.dp).background(color, RoundedCornerShape(1.dp)))
}

@Composable
private fun Figure(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label.uppercase(), fontSize = 10.5.sp, color = Palette.Muted, letterSpacing = 0.5.sp, lineHeight = 13.sp)
        Text(value, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }
}
