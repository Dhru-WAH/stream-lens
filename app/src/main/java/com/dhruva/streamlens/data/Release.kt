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

    /** "9.9K" -> 9900, "1.2M" -> 1200000, "805" -> 805; null when missing or unreadable. */
    val imdbVoteCount: Int?
        get() {
            val s = imdbVotes?.trim()?.replace(",", "")?.uppercase() ?: return null
            val mult = when (s.lastOrNull()) { 'K' -> 1_000.0; 'M' -> 1_000_000.0; else -> 1.0 }
            return s.trimEnd('K', 'M').trim().toDoubleOrNull()?.let { (it * mult).toInt() }
        }

    /**
     * Must watch: critics liked it and audiences agree in numbers. Box-office verdict is ignored on
     * purpose, so a well-loved flop still qualifies. Thresholds live in [MUST_WATCH_IMDB] / [MUST_WATCH_VOTES].
     */
    fun isMustWatch(today: LocalDate): Boolean =
        isOut(today) && reception == "Positive" &&
            (imdb ?: 0.0) >= MUST_WATCH_IMDB && (imdbVoteCount ?: 0) >= MUST_WATCH_VOTES

    /** Shown in the app at all (the feed follows these rules too; this guards against slips). */
    val isVisible: Boolean get() = lang in LANGS && !BLOCKED_PLATFORM.containsMatchIn(platform.orEmpty())

    companion object {
        val LANGS = linkedMapOf(
            "te" to "Telugu", "hi" to "Hindi", "en" to "English",
            "ta" to "Tamil", "ml" to "Malayalam", "kn" to "Kannada",
        )
        /** Languages the feed only carries when well rated or high-profile (see data/REFRESH.md). */
        val OTHER_LANGS = setOf("ta", "ml", "kn")
        private val BLOCKED_PLATFORM = Regex("apple|waves", RegexOption.IGNORE_CASE)
        const val MUST_WATCH_IMDB = 7.5
        const val MUST_WATCH_VOTES = 5_000
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
