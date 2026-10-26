package com.dhruva.streamlens.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dhruva.streamlens.data.Mark
import com.dhruva.streamlens.data.Release
import com.dhruva.streamlens.ui.theme.Display
import com.dhruva.streamlens.ui.theme.Palette
import com.dhruva.streamlens.ui.theme.SignatureFont
import java.time.LocalDate
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

// ---------- small text helpers ----------

fun shortDate(d: LocalDate) = "${d.dayOfMonth} ${d.month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)}"
fun longDate(d: LocalDate) = "${d.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)}, ${shortDate(d)}"

fun whenText(r: Release, today: LocalDate): String {
    val d = r.ott ?: return if (r.isOut(today)) "Streaming now" else "Date TBA"
    val days = ChronoUnit.DAYS.between(today, d)
    return when {
        days == 0L -> "Streaming from today"
        days < 0 -> "Since ${longDate(d)}"
        r.isExpected -> "Expected ${longDate(d)}"
        else -> "From ${longDate(d)}"
    }
}

private val REVIEWS = mapOf(
    "Positive" to Triple("Good reviews", Color(0x382FB673), Color(0xFF8FEBC0)),
    "Mixed" to Triple("Mixed reviews", Color(0x1FFFFFFF), Color(0xFFEDE4F0)),
    "Negative" to Triple("Poor reviews", Color(0x29FF6B8A), Color(0xFFFF9DB1)),
)

// ---------- chips ----------

@Composable
fun PlatformBadge(name: String, fontSize: TextUnit = 11.sp) {
    Text(
        name,
        color = Color.White,
        fontSize = fontSize,
        fontWeight = FontWeight.ExtraBold,
        maxLines = 1,
        modifier = Modifier.clip(RoundedCornerShape(5.dp)).background(Palette.platform(name)).padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

@Composable
fun Tag(text: String, color: Color = Color.White, fontSize: TextUnit = 10.5.sp, border: Color = Color.White.copy(alpha = 0.22f)) {
    Text(
        text,
        color = color,
        fontSize = fontSize,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        modifier = Modifier.clip(RoundedCornerShape(5.dp)).background(Color.Black.copy(alpha = 0.45f))
            .border(1.dp, border, RoundedCornerShape(5.dp)).padding(horizontal = 6.dp, vertical = 1.dp),
    )
}

/** Box-office verdict if there is one, otherwise the critics' mood for anything already out. */
@Composable
fun VerdictChip(r: Release, today: LocalDate, fontSize: TextUnit = 10.5.sp) {
    val v = r.verdictInfo
    if (v != null) {
        Text(
            v.label.uppercase(),
            color = Palette.tierInk(v.tier),
            fontSize = fontSize,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.5.sp,
            maxLines = 1,
            modifier = Modifier.clip(RoundedCornerShape(5.dp)).background(Palette.tierBg(v.tier)).padding(horizontal = 7.dp, vertical = 2.dp),
        )
        return
    }
    val rv = REVIEWS[r.reception]
    if (rv != null && r.isOut(today)) {
        Text(
            rv.first.uppercase(),
            color = rv.third,
            fontSize = fontSize,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.5.sp,
            maxLines = 1,
            modifier = Modifier.clip(RoundedCornerShape(5.dp)).background(rv.second)
                .border(1.dp, rv.third.copy(alpha = 0.45f), RoundedCornerShape(5.dp)).padding(horizontal = 7.dp, vertical = 1.dp),
        )
    }
}

@Composable
fun ScoreChip(label: String, value: Double, labelBg: Color, labelInk: Color, valueInk: Color, fontSize: TextUnit = 12.sp) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            label,
            color = labelInk,
            fontSize = fontSize * 0.78f,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.clip(RoundedCornerShape(3.dp)).background(labelBg).padding(horizontal = 3.dp),
        )
        Text(String.format(Locale.US, "%.1f", value), color = valueInk, fontSize = fontSize, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun Ratings(r: Release, fontSize: TextUnit = 12.sp) {
    if (r.imdb == null && r.bms == null) return
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        r.imdb?.let { ScoreChip("IMDb", it, Palette.ImdbYellow, Color.Black, Color(0xFFFFE08A), fontSize) }
        r.bms?.let { ScoreChip("BMS", it, Palette.BmsRed, Color.White, Color(0xFFFFB3C1), fontSize) }
    }
}

// ---------- signature ----------

/**
 * "by Dhruva" in the house serif. [size] is the "Dhruva" size; "by" is smaller and lighter.
 * [onDark] swaps to the lighter golds that read on the cinema-dark screens.
 */
@Composable
fun Signature(modifier: Modifier = Modifier, size: TextUnit = 16.sp, onDark: Boolean = false) {
    Text(
        buildAnnotatedString {
            withStyle(SpanStyle(fontSize = size * 0.66f, color = if (onDark) Palette.GoldMutedOnDark else Palette.GoldMuted, fontWeight = FontWeight.Medium)) { append("by ") }
            withStyle(SpanStyle(fontSize = size, color = if (onDark) Palette.GoldOnDark else Palette.Gold, fontWeight = FontWeight.SemiBold)) { append("Dhruva") }
        },
        fontFamily = SignatureFont,
        fontStyle = FontStyle.Italic,
        modifier = modifier,
    )
}

/** A row of marquee bulbs under a screen heading. */
@Composable
fun Bulbs(modifier: Modifier = Modifier, count: Int = 18) {
    Canvas(modifier.width((count * 14).dp).height(8.dp)) {
        val step = size.width / count
        for (i in 0 until count) {
            drawCircle(Palette.Bulb.copy(alpha = 0.35f), radius = 4.5f, center = Offset(step * i + step / 2, size.height / 2))
            drawCircle(Palette.Bulb, radius = 2.4f, center = Offset(step * i + step / 2, size.height / 2))
        }
    }
}

// ---------- poster card ----------

private fun titleSize(t: String) = when (t.length) {
    in 0..8 -> 19f
    in 9..14 -> 15f
    in 15..22 -> 12.5f
    in 23..32 -> 10.5f
    else -> 9f
}

@Composable
fun PosterCard(
    r: Release,
    mark: Mark?,
    today: LocalDate,
    onOpen: () -> Unit,
    onQuick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val out = r.isOut(today)
    val watched = mark?.watched == true
    var hasPhoto by remember(r.poster) { mutableStateOf(false) }
    Column(modifier.then(if (mark?.hidden == true) Modifier.padding(0.dp) else Modifier), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        BoxWithConstraints(
            Modifier.fillMaxWidth().aspectRatio(2f / 3f).clip(RoundedCornerShape(12.dp)).clickable(onClick = onOpen),
        ) {
            val cw = maxWidth.value
            PosterImage(r, r.poster, Modifier.fillMaxSize(), dimmed = watched) { hasPhoto = true }
            Box(
                Modifier.fillMaxSize().background(
                    if (hasPhoto) Brush.verticalGradient(0f to Color.Black.copy(alpha = 0.4f), 0.18f to Color.Transparent, 0.62f to Color.Transparent, 1f to Color(0xE608050A))
                    else Brush.verticalGradient(0f to Color.Black.copy(alpha = 0.35f), 0.22f to Color.Transparent, 0.45f to Color.Transparent, 1f to Color(0xEB08050A)),
                ),
            )
            Column(Modifier.fillMaxSize().padding((cw * 0.07f).dp), verticalArrangement = Arrangement.SpaceBetween) {
                FlowRow(Modifier.padding(end = 34.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    PlatformBadge(r.primaryPlatform, 10.5.sp)
                    if (r.isSeries) Tag("Series")
                    if (r.isExpected && !out) Tag("Expected", color = Palette.Bulb, border = Palette.Bulb.copy(alpha = 0.5f))
                }
                Column(verticalArrangement = Arrangement.spacedBy((cw * 0.035f).dp)) {
                    // A real poster carries its own title art, so chips and ratings move to the caption below it.
                    if (!hasPhoto) VerdictChip(r, today, fontSize = 9.5.sp)
                    if (!hasPhoto) Text(
                        r.title.uppercase(),
                        fontFamily = Display,
                        fontWeight = FontWeight.Black,
                        fontSize = (cw * titleSize(r.title) / 100f).sp,
                        lineHeight = (cw * titleSize(r.title) / 100f * 0.92f).sp,
                        color = Color.White,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (!hasPhoto) Ratings(r, fontSize = 11.sp)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(r.langName.uppercase(), color = Color(0xFFE9DFEC), fontSize = 10.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.6.sp)
                        Text((r.ott?.let { shortDate(it) } ?: "TBA").uppercase(), color = Color(0xFFE9DFEC), fontSize = 10.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.6.sp)
                    }
                }
            }
            if (watched) {
                Text(
                    "WATCHED",
                    fontFamily = Display,
                    fontWeight = FontWeight.Black,
                    color = Palette.Good,
                    fontSize = 16.sp,
                    modifier = Modifier.align(Alignment.Center).rotate(-12f)
                        .border(2.dp, Palette.Good, RoundedCornerShape(6.dp))
                        .background(Color(0x8C06281A), RoundedCornerShape(6.dp)).padding(horizontal = 10.dp, vertical = 2.dp),
                )
            }
            QuickButton(
                icon = if (out) Icons.Rounded.Check else if (mark?.watchlist == true) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                on = if (out) watched else mark?.watchlist == true,
                onBg = if (out) Palette.Good else Palette.Bulb,
                onInk = if (out) Palette.GoodInk else Palette.BulbInk,
                description = if (out) (if (watched) "Unmark watched" else "Mark watched") else (if (mark?.watchlist == true) "Remove reminder" else "Remind me"),
                onClick = onQuick,
                modifier = Modifier.align(Alignment.TopEnd).padding(7.dp),
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(r.title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Palette.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (hasPhoto && (r.verdictInfo != null || r.reception != null || r.imdb != null || r.bms != null)) FlowRow(
                Modifier.padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                VerdictChip(r, today, fontSize = 9.5.sp)
                Ratings(r, fontSize = 11.5.sp)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    r.genres.take(2).joinToString(" · ").ifEmpty { if (r.isSeries) "Series" else "Film" },
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if ((mark?.rating ?: 0) > 0) Text("★".repeat(mark!!.rating), color = Palette.Bulb, fontSize = 12.sp)
            }
        }
    }
}

/**
 * The TMDB photo when there is one, laid over the drawn artwork, so a missing poster, a blocked image
 * host or no network all quietly show the artwork instead. [onPhoto] fires once the photo has loaded.
 */
@Composable
fun PosterImage(r: Release, url: String?, modifier: Modifier = Modifier, dimmed: Boolean = false, onPhoto: () -> Unit = {}) {
    Box(modifier) {
        PosterArt(r, Modifier.fillMaxSize(), dimmed = dimmed && url == null)
        if (url != null) {
            AsyncImage(
                model = ImageRequest.Builder(LocalPlatformContext.current).data(url).crossfade(true).build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                onSuccess = { onPhoto() },
                modifier = Modifier.fillMaxSize(),
            )
            if (dimmed) Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f)))
        }
    }
}

@Composable
fun QuickButton(icon: ImageVector, on: Boolean, onBg: Color, onInk: Color, description: String, onClick: () -> Unit, modifier: Modifier = Modifier, size: Dp = 32.dp) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (on) onBg else Color(0x8C0A060C),
        border = BorderStroke(1.dp, if (on) onBg else Color.White.copy(alpha = 0.28f)),
        modifier = modifier.size(size),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = description, tint = if (on) onInk else Color.White, modifier = Modifier.size(size * 0.56f))
        }
    }
}

// ---------- buttons ----------

@Composable
fun PillButton(text: String, icon: ImageVector?, onClick: () -> Unit, filled: Boolean = false, fill: Color = Palette.Bulb, ink: Color = Palette.BulbInk, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = if (filled) fill else Color.White.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, if (filled) fill else Color.White.copy(alpha = 0.2f)),
        modifier = modifier,
    ) {
        Row(Modifier.padding(horizontal = 13.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (icon != null) Icon(icon, null, tint = if (filled) ink else Color.White, modifier = Modifier.size(17.dp))
            Text(text, color = if (filled) ink else Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

// ---------- stats ----------

@Composable
fun StatRing(stat: LangStat, modifier: Modifier = Modifier) {
    Row(
        modifier.clip(RoundedCornerShape(12.dp)).background(Palette.Surface).border(1.dp, Palette.Line, RoundedCornerShape(12.dp))
            .padding(start = 8.dp, end = 14.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        val p = if (stat.total == 0) 0f else stat.watched.toFloat() / stat.total
        Canvas(Modifier.size(38.dp)) {
            val stroke = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round)
            drawArc(Palette.Surface2, 0f, 360f, false, style = stroke, topLeft = Offset(stroke.width / 2, stroke.width / 2), size = androidx.compose.ui.geometry.Size(size.width - stroke.width, size.height - stroke.width))
            drawArc(Palette.Bulb, -90f, 360f * p, false, style = stroke, topLeft = Offset(stroke.width / 2, stroke.width / 2), size = androidx.compose.ui.geometry.Size(size.width - stroke.width, size.height - stroke.width))
        }
        Column {
            Text(stat.name.uppercase(), style = MaterialTheme.typography.labelSmall, color = Palette.Muted)
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = Palette.Ink)) { append("${stat.watched}") }
                    withStyle(SpanStyle(color = Palette.Muted)) { append(" / ${stat.total} watched") }
                },
                fontSize = 14.sp,
            )
        }
    }
}

@Composable
fun SectionHeader(label: String, badge: String?, count: Int, modifier: Modifier = Modifier) {
    Row(modifier.padding(top = 22.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(label.uppercase(), style = MaterialTheme.typography.headlineSmall)
        if (badge != null) Text(
            badge,
            color = Palette.BulbInk,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(Palette.Bulb).padding(horizontal = 8.dp, vertical = 2.dp),
        )
        Text("$count title${if (count == 1) "" else "s"}", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun EmptyState(title: String, body: String, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxWidth().padding(top = 28.dp).clip(RoundedCornerShape(16.dp)).background(Palette.Bg2)
            .border(1.dp, Palette.Line, RoundedCornerShape(16.dp)).padding(vertical = 36.dp, horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(6.dp))
        Text(body, style = MaterialTheme.typography.bodyMedium, color = Palette.Muted, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}
