# StreamLens

Personal, sideloaded Android app: what's new and coming on Indian OTT platforms (Telugu first, then Hindi
a few English films, and well-rated or high-profile Tamil, Malayalam and Kannada films), with ratings, box-office verdicts, budgets and my own watched list.

## Install
Built on the Windows PC into `Downloads/streamlens-vX.Y.apk`, then sent to the phone with Quick Share
(v1.4 and earlier: `Google Drive › App installs › StreamLens`).

## How it works
- **Release list:** `data/releases.json` in this repo. The app downloads it from
  `raw.githubusercontent.com` on launch and pull-to-refresh, and keeps the last copy for offline use.
- **Daily refresh:** _paused: it ran on the old Mac; last run by hand on 12 Nov 2026._ A scheduled Claude task on my Mac (`streamlens-daily-refresh`, ~11 am, or on next wake if
  the Mac was asleep) follows [`data/REFRESH.md`](data/REFRESH.md): it searches the web for new and upcoming
  releases, fills IMDb / BookMyShow ratings, verdicts and budgets, and pushes the file.
- **My data stays on the phone:** watched ticks, watchlist, star ratings and "not interested" live in a Room
  database. Settings › Backup writes them to a JSON file; Restore reads one back.
- **Alerts:** a daily background check (~7 pm) posts a notification when a watchlist title starts streaming, and
  (optionally) when new Telugu titles arrive.
- **Must watch:** the app stamps a title "Must watch" when it is streaming, critics were positive, and IMDb is
  7.5+ from at least 5K votes (box-office verdict ignored). Thresholds: `MUST_WATCH_*` in `Release.kt`.
- **Posters** are drawn by the app (genre colours + title type), so there's no image hosting to maintain.

## Build (Windows PC, since v1.5)
Android SDK lives in `D:\Claude\android-sdk` (`local.properties` points at it); JDK 20 runs Gradle.
Java must be forced onto IPv4 or Gradle downloads time out on this network. From Git Bash:
```
export JAVA_HOME='C:\Program Files\Java\jdk-20' GRADLE_OPTS=-Djava.net.preferIPv4Stack=true
./gradlew assembleDebug -Dorg.gradle.jvmargs="-Xmx3g -Djava.net.preferIPv4Stack=true"
cp app/build/outputs/apk/debug/app-debug.apk ~/Downloads/streamlens-vX.Y.apk   # then Quick Share to the phone
```
Bump `versionCode`/`versionName` every build.

**Signing:** builds are signed with this PC's debug keystore (`%USERPROFILE%\.android\debug.keystore`).
v1.4 and earlier were signed on the old Mac, so v1.5 had to be a fresh install (Backup, uninstall, install,
Restore). Keep a copy of that keystore: losing it means another uninstall for the next update.
