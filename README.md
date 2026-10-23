# SaifNest

SaifNest is a local-first website media discovery and download helper with a Flask web UI, Chromium-assisted media request sniffing, live scan events, scan range/concurrency controls, a download queue, and a separate Android Media3/ExoPlayer companion starter.

> **Status:** Development build. Run the checks and test against sites you own or are authorized to access before relying on it. Website inventory counts can be estimates when a sitemap is incomplete. The Android companion is source scaffolding and is not yet a verified APK.

## Features

- Fast inventory using sitemap URLs and a clearly distinguished estimated count.
- Chromium-assisted discovery of media request URLs, URL deduplication, and live event updates.
- Scan range and bounded concurrency controls with cooperative pause/resume.
- Download queue with duplicate handling and yt-dlp retry/resume options.
- Web player controls including fullscreen, seek gestures, playback speed, and available subtitle tracks.
- Optional Android Media3/ExoPlayer companion launch through the `saifnest://play?url=...` deep link.
- Public HTTP(S) URL validation that blocks localhost/private network targets.

## Requirements

- Python 3.10+
- Flask 3.x
- yt-dlp
- Playwright Python package and a compatible Chromium installation for browser-based discovery
- Optional: ffmpeg and ffprobe for thumbnail generation
- Android companion build: JDK 17, Android SDK platform 35, compatible Gradle/Android Gradle Plugin, and dependency network access

## Run on Termux

1. Install Python and the browser dependencies required by your Termux setup.
2. Extract this repository and enter the project directory.
3. Install Python packages:

   ```sh
   pip install -r requirements.txt
   ```

4. Run:

   ```sh
   chmod +x run.sh
   ./run.sh
   ```

If the default Android shared-storage download directory is unavailable, set `SAIFNEST_DOWNLOAD_DIR` to a writable directory.

## Android companion

The `android-player/` directory contains a separate Media3/ExoPlayer starter. See [android-player/README.md](android-player/README.md). The WebUI deep link does not install the Android companion automatically.

## Validation

The implementation report in `IMPLEMENTATION-AND-TEST-REPORT.txt` describes syntax and smoke checks and explicitly lists checks that have not been performed. A passing syntax check is not proof of end-to-end runtime success.

## Responsible use

Use SaifNest only for media you own or are authorized to access/download. It does not aim to bypass DRM, paywalls, authentication, or access restrictions.

## License

MIT — see [LICENSE](LICENSE).
