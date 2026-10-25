package com.dhruva.streamlens.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.dhruva.streamlens.data.Release
import kotlin.math.max

/**
 * The poster for a title: a hue pair hashed from the title (same hash as the web page, so colours match)
 * and a texture chosen by genre. No image hosting, and every title gets one.
 */
private val GENRE_HUE = mapOf(
    "action" to 12f, "thriller" to 198f, "crime" to 222f, "mystery" to 242f, "horror" to 352f,
    "romance" to 330f, "comedy" to 44f, "family" to 120f, "drama" to 268f, "scifi" to 184f,
    "fantasy" to 288f, "sports" to 150f, "biography" to 30f, "animation" to 170f, "musical" to 300f, "war" to 60f,
)

private fun genreKey(r: Release): String {
    for (g in r.genres) {
        val k = g.lowercase().filter { it in 'a'..'z' }
        when {
            k.startsWith("sci") -> return "scifi"
            k == "romcom" -> return "romance"
            k == "biopic" -> return "biography"
            k in GENRE_HUE -> return k
        }
    }
    return "drama"
}

/** FNV-1a over UTF-16 code units, matching the page's JS `hash()`. */
private fun hash(s: String): Long {
    var h = 2166136261L
    for (c in s) {
        h = h xor c.code.toLong()
        h = (h * 16777619L) and 0xFFFFFFFFL
    }
    return h
}

private data class Art(val key: String, val h1: Float, val h2: Float)

private fun artFor(r: Release): Art {
    val h = hash(r.title)
    val key = genreKey(r)
    val h1 = ((GENRE_HUE.getValue(key) + (h % 50) - 25 + 360) % 360).toFloat()
    val h2 = ((h1 + 40 + ((h shr 8) % 70)) % 360)
    return Art(key, h1, h2)
}

@Composable
fun PosterArt(release: Release, modifier: Modifier = Modifier, dimmed: Boolean = false) {
    val art = remember(release.id, release.title, release.genres) { artFor(release) }
    Canvas(modifier) {
        drawRect(Brush.linearGradient(listOf(Color.hsl(art.h1, 0.45f, 0.20f), Color.hsl(art.h2, 0.50f, 0.09f)), Offset.Zero, Offset(size.width, size.height)))
        drawRect(Brush.radialGradient(listOf(Color.hsl(art.h1, 0.78f, 0.58f, 0.95f), Color.Transparent), Offset(size.width * 0.18f, size.height * 0.12f), max(size.width, size.height) * 0.62f))
        drawRect(Brush.radialGradient(listOf(Color.hsl(art.h2, 0.80f, 0.48f, 0.95f), Color.Transparent), Offset(size.width * 0.92f, size.height * 0.96f), max(size.width, size.height) * 0.62f))
        texture(art.key)
        drawRect(Brush.radialGradient(listOf(Color.White.copy(alpha = 0.16f), Color.Transparent), Offset(size.width * 0.7f, size.height * 0.3f), size.width * 0.55f), blendMode = BlendMode.Screen)
        if (dimmed) drawRect(Color.Black.copy(alpha = 0.55f))
    }
}

private fun DrawScope.texture(key: String) {
    val w = size.width
    val h = size.height
    val white = Color.White
    when (key) {
        "action" -> {
            var x = -h
            while (x < w) {
                drawLine(white.copy(alpha = 0.16f), Offset(x, h), Offset(x + h * 0.55f, 0f), strokeWidth = 3f)
                x += 44f
            }
        }
        "thriller", "crime", "mystery" -> {
            var y = 0f
            while (y < h) {
                drawRect(Color.Black.copy(alpha = 0.28f), Offset(0f, y), androidx.compose.ui.geometry.Size(w, 2f))
                y += 7f
            }
            drawRect(Brush.radialGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)), Offset(w / 2, h * 0.38f), max(w, h) * 0.75f))
        }
        "horror" -> {
            for (i in 1..14) drawCircle(white.copy(alpha = 0.06f), radius = i * 12f, center = Offset(w / 2, h * 0.3f), style = Stroke(2f))
            drawRect(Brush.radialGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)), Offset(w / 2, h * 0.3f), max(w, h) * 0.7f))
        }
        "romance" -> {
            drawCircle(white.copy(alpha = 0.22f), w * 0.14f, Offset(w * 0.3f, h * 0.7f))
            drawCircle(white.copy(alpha = 0.14f), w * 0.2f, Offset(w * 0.7f, h * 0.45f))
            drawCircle(white.copy(alpha = 0.1f), w * 0.09f, Offset(w * 0.45f, h * 0.25f))
        }
        "comedy", "family", "animation" -> {
            var y = 8f
            while (y < h) {
                var x = 8f
                while (x < w) { drawCircle(white.copy(alpha = 0.22f), 2.2f, Offset(x, y)); x += 16f }
                y += 16f
            }
        }
        "scifi", "fantasy" -> drawRect(
            Brush.sweepGradient(
                listOf(Color.Transparent, white.copy(alpha = 0.22f), Color.Transparent, white.copy(alpha = 0.12f), Color.Transparent),
                Offset(w * 0.6f, h * 0.4f),
            ),
        )
        "sports", "biography" -> {
            var x = 0f
            while (x < w) { drawLine(white.copy(alpha = 0.1f), Offset(x, 0f), Offset(x, h), 2f); x += 26f }
        }
        else -> {
            drawLine(white.copy(alpha = 0.12f), Offset(w * 0.62f, 0f), Offset(w * 0.62f, h), 1.5f)
            drawLine(white.copy(alpha = 0.08f), Offset(0f, h * 0.3f), Offset(w, h * 0.3f), 1.5f)
        }
    }
}
