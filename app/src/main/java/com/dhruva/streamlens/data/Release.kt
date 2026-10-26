package com.dhruva.streamlens.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.format.DateTimeParseException

/** `data/releases.json` in the stream-lens repo. Schema documented in data/REFRESH.md. */
@Serializable
data class Feed(
    val schema: Int = 1,
    val updatedAt: String? = null,
    val count: Int = 0,
    val titles: List<Release> = emptyList(),
)

@Serializable
data class Release(
    val id: String,
    val title: String,
    val year: Int? = null,
    val lang: String = "te",
    val kind: String = "movie",
    val platform: String? = null,
    val ottDate: String? = null,
    val dateConfirmed: Boolean? = null,
    val status: String? = null,
    val theatricalDate: String? = null,
    val genres: List<String> = emptyList(),
    val cast: String? = null,
    val director: String? = null,
    val dubs: List<String> = emptyList(),
    val synopsis: String? = null,
    val source: String? = null,
    val imdb: Double? = null,
    val imdbVotes: String? = null,
    val bms: Double? = null,
    val bmsVotes: String? = null,
    val ratingSource: String? = null,
    val reception: String? = null,
    val criticScore: String? = null,
    val verdict: String? = null,
    val verdictSource: String? = null,
    val boxOffice: String? = null,
    val money: String? = null,
    val budgetAmt: Double? = null,
    val grossAmt: Double? = null,
    val breakEvenAmt: Double? = null,
    val breakEvenKind: String? = null,
    val budgetNote: String? = null,
    val budgetSource: String? = null,
    /** TMDB image URLs (filled by tools/posters.py); null means the app draws its own artwork. */
    val poster: String? = null,
    val backdrop: String? = null,
    val tmdbId: Long? = null,
    val tmdbType: String? = null,
) {
    val ott: LocalDate? get() = ottDate.toDate()
    val theatrical: LocalDate? get() = theatricalDate.toDate()

    /** Streaming already: dated on or before today, or undated but marked streaming. */
    fun isOut(today: LocalDate): Boolean = ott?.let { !it.isAfter(today) } ?: (status == "streaming")

    /** "Prime Video (Rent) / Aha" -> ["Prime Video", "Aha"]. */
    val platforms: List<String>
        get() = platform.orEmpty().split('/', ',', '&', '+')
            .map { it.replace(Regex("\\s*\\(.*?\\)\\s*"), "").trim() }
            .filter { it.isNotEmpty() }

    val primaryPlatform: String get() = platforms.firstOrNull() ?: "TBA"
    val langName: String get() = LANGS[lang] ?: lang
    val isSeries: Boolean get() = kind == "series"
    val isExpected: Boolean get() = ott != null && dateConfirmed == false
    val hasTeluguAudio: Boolean get() = lang == "te" || "te" in dubs

    /** One number to sort "top rated" by: IMDb first, BookMyShow if that's all there is. */
    val score: Double get() = imdb ?: bms ?: -1.0
    val verdictInfo: Verdict? get() = Verdict.of(verdict)

    /** Shown in the app at all (the feed follows these rules too; this guards against slips). */
    val isVisible: Boolean get() = lang in LANGS && !BLOCKED_PLATFORM.containsMatchIn(platform.orEmpty())

    companion object {
        val LANGS = linkedMapOf("te" to "Telugu", "hi" to "Hindi", "en" to "English")
        private val BLOCKED_PLATFORM = Regex("apple|waves", RegexOption.IGNORE_CASE)
    }
}

enum class Tier { TOP, HIT, AVG, LOW, BAD }

enum class Verdict(val raw: String, val label: String, val tier: Tier) {
    ATB("All Time Blockbuster", "All-time blockbuster", Tier.TOP),
    BLOCKBUSTER("Blockbuster", "Blockbuster", Tier.TOP),
    SUPER_HIT("Super Hit", "Super hit", Tier.HIT),
    HIT("Hit", "Hit", Tier.HIT),
    ABOVE_AVG("Above Average", "Above average", Tier.AVG),
    AVERAGE("Average", "Average", Tier.AVG),
    BELOW_AVG("Below Average", "Below average", Tier.LOW),
    FLOP("Flop", "Flop", Tier.LOW),
    DISASTER("Disaster", "Disaster", Tier.BAD);

    /** Higher is bigger: All-time blockbuster 9 … Disaster 1. */
    val rank: Int get() = entries.size - ordinal

    companion object {
        fun of(raw: String?): Verdict? = entries.firstOrNull { it.raw.equals(raw, ignoreCase = true) }
    }
}

val FeedJson = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
    coerceInputValues = true
    isLenient = true
}

private fun String?.toDate(): LocalDate? = try {
    this?.takeIf { it.length >= 10 }?.let { LocalDate.parse(it.substring(0, 10)) }
} catch (_: DateTimeParseException) {
    null
}
