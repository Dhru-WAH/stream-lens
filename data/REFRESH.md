# Daily refresh: instructions for the scheduled Claude job

You keep `data/releases.json` current for StreamLens, Dhruva's personal OTT release tracker (India).
The Android app downloads this file from
`https://raw.githubusercontent.com/Dhru-WAH/stream-lens/main/data/releases.json`, so every change you
push reaches the phone. Work only inside this repository and touch no file other than
`data/releases.json` (and this file, if the rules here need a fix).

## What belongs in the list

- **Languages:** Telugu (`te`): all of them, including small and direct-to-OTT films. Hindi (`hi`):
  theatrical films reaching OTT plus notable direct-to-OTT films. English (`en`): only notable,
  high-profile films available in India.
- **Never include** Tamil, Malayalam, Kannada or any other language, even when dubbed into Telugu.
  Never include titles whose only platform is Apple TV or WAVES OTT.
- **Web series** go in too, with `"kind": "series"` (the app shows Movies and Series separately):
  Telugu: all notable original series and new seasons (Aha, ETV Win, ZEE5, JioHotstar, Prime Video, Netflix…);
  Hindi: notable series and new seasons, especially from the big platforms; English: only a few very big ones
  available in India (major Netflix / Prime Video / JioHotstar shows). A new season is its own record, e.g.
  title `"Mirzapur Season 4"`, id `mirzapur-season-4-2026`. Use the season premiere date as `ottDate`.
  Skip reality shows, talk shows, kids' shows and documentaries unless they are very big.
  For series, leave `verdict`, `budgetAmt`, `grossAmt` and `breakEvenAmt` null; fill IMDb, reception and
  `criticScore` when known.
- Platforms as named in India: Netflix, Prime Video, JioHotstar, ZEE5, SonyLIV, Aha, ETV Win, Sun NXT.
  A rent-only title is `"Prime Video (Rent)"`.

## Each run

1. Read `data/releases.json`.
2. **Find new titles**: search the web for OTT releases in India since `updatedAt`, plus newly announced
   upcoming dates (through roughly 8 weeks ahead). Good sources: 123telugu, Gulte, Greatandhra,
   OTTplay, Sacnilk, Pinkvilla, Indian Express, Koimoi, Bollywood Hungama, platform announcements.
3. **Update existing titles**: OTT dates that got confirmed or moved, upcoming titles that are now
   streaming, platform changes.
4. **Fill ratings and money** for titles released in the last ~10 weeks or missing them:
   IMDb (`imdb`, `imdbVotes`; JustWatch pages quote IMDb when imdb.com blocks fetching),
   BookMyShow (`bms`, `bmsVotes`) when visible, box-office `verdict`, worldwide `grossAmt`,
   `budgetAmt`, `breakEvenAmt` and `breakEvenKind`, critic `reception` / `criticScore`.
   Films still in cinemas: update the gross; give a verdict only once the run is effectively over.
5. Drop titles whose `ottDate` is more than 120 days old **and** that have nothing new to add; keep
   everything else.
6. Set `updatedAt` to now (IST, ISO 8601), set `count`, keep the list sorted by `ottDate` newest first
   (no-date titles first), validate (see below), then commit and push to `main` with a message such as
   `Refresh: +3 new, 5 updated (2026-10-27)`. If nothing changed, do not commit.

Rules: never guess. Unknown is `null` (or leave the key out). Use only facts you found in a source and
put a URL in `source`, `ratingSource`, `verdictSource` or `budgetSource`. Don't rerun searches for
titles that are already complete. Keep total web searches under about 150 per run.

## Record format

```json
{
  "id": "the-paradise-2026",            // slug(title)-year, stable forever; never rename an id
  "title": "The Paradise", "year": 2026,
  "lang": "te",                          // te | hi | en
  "kind": "movie",                       // movie | series
  "platform": "Netflix",
  "ottDate": "2026-10-22",               // YYYY-MM-DD, null if not announced
  "dateConfirmed": true,                 // false = reported/expected only
  "status": "streaming",                 // streaming | upcoming (only matters when ottDate is null)
  "theatricalDate": "2026-09-24",        // null for direct-to-OTT
  "genres": ["Action", "Drama"], "cast": "Nani, …", "director": "Srikanth Odela",
  "dubs": ["hi"],                        // other audio languages (te/hi/en only matter to the app)
  "synopsis": "One plain sentence in your own words.",
  "source": "https://…",
  "imdb": 6.4, "imdbVotes": "9.9K", "ratingSource": "https://…",
  "bms": 6.7, "bmsVotes": "6.2K",
  "reception": "Mixed",                  // Positive | Mixed | Negative
  "criticScore": "123telugu 2.75/5",
  "verdict": "Below Average",            // All Time Blockbuster | Blockbuster | Super Hit | Hit |
                                         // Above Average | Average | Below Average | Flop | Disaster
  "verdictSource": "https://…",
  "boxOffice": "₹174 cr worldwide",     // short human text
  "money": "INR",                        // INR (amounts in ₹ crore) | USD (amounts in $ millions)
  "budgetAmt": 175, "grossAmt": 174, "breakEvenAmt": 105,
  "breakEvenKind": "share",              // gross | share | unclear
  "budgetNote": "₹150–200 cr reported", "budgetSource": "https://…"
}
```

Top level: `{"schema": 1, "updatedAt": "…", "count": N, "titles": [ … ]}`.

## Validate before pushing

```bash
python3 -c "import json;d=json.load(open('data/releases.json'));t=d['titles'];ids=[x['id'] for x in t];assert len(ids)==len(set(ids));assert all(x['lang'] in ('te','hi','en') for x in t);assert d['count']==len(t);print('ok',len(t))"
```
