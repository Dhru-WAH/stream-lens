# StreamLens

Personal, sideloaded Android app: what's new and coming on Indian OTT platforms (Telugu first, then Hindi
a few English films, and well-rated or high-profile Tamil, Malayalam and Kannada films), with ratings, box-office verdicts, budgets and my own watched list.

## Install
v1.5 was built on the Windows PC and sent to the phone with Quick Share. From now on it is built on the Mac and
installed with `adb install -r` (see [CLAUDE.md](CLAUDE.md)).

## How it works
- **Release list:** `data/releases.json` in this repo. The app downloads it from
  `raw.githubusercontent.com` on launch and pull-to-refresh, and keeps the last copy for offline use.
- **Daily refresh:** a scheduled Claude task on my Mac (`streamlens-daily-refresh`, ~11 am, or the next time
  the Claude app is open if it was closed) follows [`data/REFRESH.md`](data/REFRESH.md): it searches the web for new and upcoming
  releases, fills IMDb / BookMyShow ratings, verdicts and budgets, and pushes the file.
- **My data stays on the phone:** watched ticks, watchlist, star ratings and "not interested" live in a Room
  database. Settings › Backup writes them to a JSON file; Restore reads one back.
- **Alerts:** a daily background check (~7 pm) posts a notification when a watchlist title starts streaming, and
  (optionally) when new Telugu titles arrive.
- **Must watch:** the app stamps a title "Must watch" when it is streaming, critics were positive, and IMDb is
  7.5+ from at least 5K votes (box-office verdict ignored). Thresholds: `MUST_WATCH_*` in `Release.kt`.
- **Posters** come from TMDB (filled into the feed by `tools/posters.py`); titles without a match get artwork
  drawn by the app (genre colours + title type), so there's no image hosting to maintain.

## Developing
**Start with [CLAUDE.md](CLAUDE.md).** It has the full handoff: the code map, the data/app contract, open
items (the daily refresh needs re-scheduling on the Mac), how to build on the Mac and the signing key to carry
over. Claude Code reads it automatically when you open this repo.
