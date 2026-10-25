# StreamLens

Personal, sideloaded Android app: what's new and coming on Indian OTT platforms (Telugu first, then Hindi
and a few English films), with ratings, box-office verdicts, budgets and my own watched list.

## Install
Install the APK from `Google Drive › App installs › StreamLens`.

## How it works
- **Release list:** `data/releases.json` in this repo. The app downloads it from
  `raw.githubusercontent.com` on launch and pull-to-refresh, and keeps the last copy for offline use.
- **Daily refresh:** a scheduled Claude job follows [`data/REFRESH.md`](data/REFRESH.md): it searches the web
  for new and upcoming releases, fills IMDb / BookMyShow ratings, verdicts and budgets, and pushes the file.
- **My data stays on the phone:** watched ticks, watchlist, star ratings and "not interested" live in a Room
  database. Settings › Backup writes them to a JSON file; Restore reads one back.
- **Alerts:** a daily background check posts a notification when a watchlist title starts streaming, and
  (optionally) when new Telugu titles arrive.
- **Posters** are drawn by the app (genre colours + title type), so there's no image hosting to maintain.

## Build
```
export JAVA_HOME=/opt/homebrew/opt/openjdk@17
./gradlew assembleDebug
rclone copyto app/build/outputs/apk/debug/app-debug.apk "gdrive:App installs/StreamLens/streamlens-vX.Y.apk"
```
Bump `versionCode`/`versionName` every build. Signed with the shared debug keystore.
