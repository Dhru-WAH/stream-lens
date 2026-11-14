# CLAUDE.md: handoff notes for StreamLens

Read this before changing anything. [README.md](README.md) is the user-facing overview, and
[data/REFRESH.md](data/REFRESH.md) is the exact procedure for the data refresh. This file covers what a
developer (you) needs in order to continue: how the pieces fit, the rules, and how to build and ship from
the Mac. Update it in the same commit whenever any of that changes.

## What this is
A personal, sideloaded Android app for **Dhruva**: what's new and coming on Indian OTT platforms. Telugu
comes first, then Hindi, notable English titles, and well-rated or high-profile Tamil, Malayalam and Kannada
titles. Each title shows ratings, box-office verdict and budget, and Dhruva keeps a private watched list,
watchlist and star ratings.

- Package `com.dhruva.streamlens`, minSdk 26, target/compile SDK 37. Current version **1.5 (versionCode 6)**.
- Repo `Dhru-WAH/stream-lens` (public). The app downloads `data/releases.json` from `main` via
  raw.githubusercontent.com, so **anything pushed to `data/releases.json` on `main` goes live on the phone**.

## Two independent parts
1. **The data:** `data/releases.json`, maintained by a Claude job that follows `data/REFRESH.md` (web search,
   fill ratings and money, run `tools/posters.py`, validate, commit `Refresh: +N new, M updated (date)`).
2. **The app:** reads that file. It has no backend and no accounts. The user's own marks never leave the phone.

You can change one without the other, but the JSON schema is the contract between them. Add new fields as
optional with defaults (`Release` uses `ignoreUnknownKeys`), never rename or remove fields, and never
change an existing `id`.

## Code map
```
app/src/main/java/com/dhruva/streamlens/
  StreamLensApp.kt        Application: holds feed repo, prefs, Room db; schedules SyncWorker; notification channel
  data/Release.kt         Feed/Release models + derived logic (isOut, isMustWatch, verdict tiers, LANGS,
                          OTHER_LANGS, blocked platforms Apple TV / WAVES). MUST_WATCH_IMDB=7.5, _VOTES=5000
  data/FeedRepository.kt  download FEED_URL -> cache in filesDir; first launch seeds from assets/releases.json
  data/Marks.kt           Room: Mark(id, watched, watchlist, rating, hidden...) = the user's own data (schema in app/schemas)
  data/Prefs.kt           SharedPreferences: Tab (STREAMING/UPCOMING/WATCHLIST/WATCHED), Sort, Filters, AlertPrefs
  data/Backup.kt          Settings > Backup/Restore marks+prefs as a JSON file (used when reinstalling)
  ui/AppViewModel.kt      combines feed + marks + filters into HomeState sections (compute()), pick-for-tonight
  ui/HomeScreen.kt        tabs, filters sheet, search, poster grid; ui/DetailSheet.kt title details
  ui/Components.kt        PosterCard (TMDB photo via Coil over drawn art), chips, rating rows
  ui/PosterArt.kt         drawn fallback artwork (genre colours + type) when no TMDB poster
  ui/SettingsScreen.kt    alerts, backup/restore, about; ui/theme/Theme.kt Palette + fonts (Archivo, Cormorant)
  work/SyncWorker.kt      daily WorkManager job at ~19:00: refresh feed, then Alerts.afterSync
  work/Alerts.kt          notifications: watchlist title started streaming; optional new-title alerts per language
app/src/main/assets/releases.json   bundled snapshot for first launch (refresh it when releasing a new APK)
tools/posters.py        TMDB poster/backdrop filler used by the refresh job
data/REFRESH.md         the refresh job's instructions (inclusion rules, record format, validation)
```
Splash screen and house brand: on Android 12+ the launch screen is ivory with a "by Dhruva" signature
(`values-v31/themes.xml`, `drawable/splash_branding.xml`). Keep it.

## Version history (from git)
- 1.1: refresh moved to the Mac; alert check at 7 pm.
- 1.2: calmer home screen.
- 1.3: Movies and Web series browsed separately.
- 1.4: real TMDB posters on tiles and in details.
- 1.5 (2026-11-12): "Must watch" stamp; Tamil, Malayalam and Kannada titles; first build on Windows.

## Daily refresh (scheduled on the new Mac, 2026-11-14)
The Claude scheduled task `streamlens-daily-refresh` runs daily at ~11 am IST (or when the Claude app next
opens). It works in the clone at `~/Claude/apps/stream-lens`, pulls `main`, follows `data/REFRESH.md` and
pushes. Its prompt lives in `~/.claude/scheduled-tasks/streamlens-daily-refresh/SKILL.md`.

## Open items
- **TMDB token:** `tools/posters.py` reads `$TMDB_TOKEN` or `~/.config/streamlens/tmdb_token`. It was on the
  old Mac only and isn't on the new Mac yet. Dhruva needs to create or copy a TMDB API Read Access Token on
  the Mac. Never commit it. Without it, the refresh still works, but new titles get drawn art instead of
  posters.
- `python3` is needed for the poster script and the validation one-liner. macOS has it; the Windows PC
  didn't, so Node was used there.

## Building on the Mac
The toolchain is AGP 9.4.1, Gradle 9.8.0 (wrapper), Kotlin 2.4.20, KSP 2.3.12 (for Room) and Compose BOM
2026.09.00. These are the same as Dhruva's other app, `Dhru-WAH/jenkins`.
1. Install JDK 17 or newer (e.g. `brew install --cask temurin@21`) and the Android SDK (Android Studio, or
   `brew install --cask android-commandlinetools` then
   `sdkmanager "platforms;android-37.0" "build-tools;37.0.0" "platform-tools"`).
2. `local.properties` (git-ignored): `sdk.dir=/Users/<you>/Library/Android/sdk`
3. Bump `versionCode` and `versionName` in `app/build.gradle.kts`. Copy the current `data/releases.json` to
   `app/src/main/assets/releases.json` so a fresh install starts with recent data.
4. `./gradlew assembleDebug` → `app/build/outputs/apk/debug/app-debug.apk`. Ship it as `streamlens-vX.Y.apk`.
   The `-Djava.net.preferIPv4Stack=true` in the README and `gradle.properties` was a workaround for the
   Windows network; it is harmless on the Mac.
5. Install: `adb install -r app/build/outputs/apk/debug/app-debug.apk` (USB, or wireless debugging with
   `adb pair`). The Windows PC used Quick Share, which a Mac can't do.
6. Commit as `vX.Y: <what changed>` and push to `main`.

### Signing (important)
An update must be signed with the same key as the installed app.
- v1.5 is signed with the Windows PC's `~/.android/debug.keystore`. Its SHA-256 certificate fingerprint is
  `52:92:66:E8:2E:A0:83:D2:1B:A2:DD:D0:A6:D4:F2:D6:B1:13:8B:31:B5:73:DB:AB:55:61:2D:3E:4B:A4:4D:4B`.
- Copy that keystore to the Mac as `~/.android/debug.keystore` before the first Mac build, then check it with
  `keytool -list -v -keystore ~/.android/debug.keystore -storepass android | grep SHA256`.
- If the key differs, updating fails. The way out is Settings › Backup in the app, uninstall, install,
  then Restore. That already happened once (v1.4 → v1.5), when the old Mac's key was lost.

## Rules to keep
- Never commit tokens or keystores.
- For data edits, follow `data/REFRESH.md` exactly: never guess, cite sources, keep ids stable, and validate
  before pushing.
- Keep user data on the phone. No analytics, no accounts, no network except the feed and TMDB images.
- UI tone: calm, poster-first, dark Palette. Read `ui/theme/Theme.kt` before adding colours or type styles.
