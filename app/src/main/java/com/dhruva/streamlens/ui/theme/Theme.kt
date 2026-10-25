package com.dhruva.streamlens.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.dhruva.streamlens.R
import com.dhruva.streamlens.data.Tier

/** Cinema night: plum-black hall, marquee-bulb amber. Deliberately dark-only. */
object Palette {
    val Bg = Color(0xFF120D16)
    val Bg2 = Color(0xFF18121D)
    val Surface = Color(0xFF1E1724)
    val Surface2 = Color(0xFF2A2131)
    val Line = Color(0xFF352B3D)
    val Ink = Color(0xFFF5EFF6)
    val Muted = Color(0xFFAC9FB5)
    val Dim = Color(0xFF7B6F84)
    val Bulb = Color(0xFFFFB547)
    val BulbInk = Color(0xFF2A1700)
    val Good = Color(0xFF5AD39A)
    val GoodInk = Color(0xFF06281A)
    val Rose = Color(0xFFFF6B8A)
    val ImdbYellow = Color(0xFFF5C518)
    val BmsRed = Color(0xFFF84464)

    // House brand (signature only)
    val Gold = Color(0xFFA8844C)
    val GoldMuted = Color(0xFFB9A07A)
    val Ivory = Color(0xFFF7F4EE)
    val IvoryInk = Color(0xFF3B3226)

    fun tierBg(t: Tier) = when (t) {
        Tier.TOP -> Color(0xFFFFC85F)
        Tier.HIT -> Color(0xFF2FB673)
        Tier.AVG -> Color(0xFF5E5474)
        Tier.LOW -> Color(0xFFE86A43)
        Tier.BAD -> Color(0xFFC0183A)
    }

    fun tierInk(t: Tier) = when (t) {
        Tier.TOP -> Color(0xFF2A1700)
        Tier.HIT -> Color(0xFF04220F)
        Tier.AVG -> Color(0xFFF5EFF6)
        Tier.LOW -> Color(0xFF2A0A00)
        Tier.BAD -> Color.White
    }

    private val platformColors = listOf(
        Regex("netflix", RegexOption.IGNORE_CASE) to Color(0xFFC70A12),
        Regex("prime", RegexOption.IGNORE_CASE) to Color(0xFF0A7FC0),
        Regex("hotstar", RegexOption.IGNORE_CASE) to Color(0xFF1B3FD1),
        Regex("^aha", RegexOption.IGNORE_CASE) to Color(0xFFE55B00),
        Regex("zee5", RegexOption.IGNORE_CASE) to Color(0xFF7B22C2),
        Regex("sony", RegexOption.IGNORE_CASE) to Color(0xFFB7720C),
        Regex("etv", RegexOption.IGNORE_CASE) to Color(0xFFC21A22),
        Regex("sun\\s*nxt", RegexOption.IGNORE_CASE) to Color(0xFFD9650F),
        Regex("mx", RegexOption.IGNORE_CASE) to Color(0xFF1E5FC9),
        Regex("youtube", RegexOption.IGNORE_CASE) to Color(0xFFCC0000),
    )

    fun platform(name: String): Color = platformColors.firstOrNull { it.first.containsMatchIn(name) }?.second ?: Color(0xFF4A3F55)
}

val Display = FontFamily(Font(R.font.archivo_condensed_black, FontWeight.Black))
val Body = FontFamily(
    Font(R.font.archivo_regular, FontWeight.Normal),
    Font(R.font.archivo_semibold, FontWeight.SemiBold),
    Font(R.font.archivo_extrabold, FontWeight.ExtraBold),
)
val SignatureFont = FontFamily(
    Font(R.font.cormorant_italic_medium, FontWeight.Medium, FontStyle.Italic),
    Font(R.font.cormorant_italic_semibold, FontWeight.SemiBold, FontStyle.Italic),
)

private val base = TextStyle(fontFamily = Body, color = Palette.Ink)
private val typography = Typography(
    displaySmall = TextStyle(fontFamily = Display, fontWeight = FontWeight.Black, fontSize = 40.sp, lineHeight = 38.sp),
    headlineSmall = TextStyle(fontFamily = Display, fontWeight = FontWeight.Black, fontSize = 26.sp, lineHeight = 26.sp),
    titleLarge = base.copy(fontWeight = FontWeight.ExtraBold, fontSize = 20.sp),
    titleMedium = base.copy(fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
    titleSmall = base.copy(fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
    bodyLarge = base.copy(fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = base.copy(fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = base.copy(fontSize = 12.5.sp, lineHeight = 17.sp, color = Palette.Muted),
    labelLarge = base.copy(fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
    labelMedium = base.copy(fontWeight = FontWeight.SemiBold, fontSize = 12.sp),
    labelSmall = base.copy(fontWeight = FontWeight.SemiBold, fontSize = 11.sp, letterSpacing = 0.6.sp),
)

@Composable
fun StreamLensTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Palette.Bulb,
            onPrimary = Palette.BulbInk,
            secondary = Palette.Good,
            background = Palette.Bg,
            onBackground = Palette.Ink,
            surface = Palette.Bg2,
            onSurface = Palette.Ink,
            surfaceVariant = Palette.Surface,
            onSurfaceVariant = Palette.Muted,
            surfaceContainer = Palette.Surface,
            surfaceContainerHigh = Palette.Surface2,
            surfaceContainerLow = Palette.Bg2,
            outline = Palette.Line,
            outlineVariant = Palette.Line,
        ),
        typography = typography,
        content = content,
    )
}
