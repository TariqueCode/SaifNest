# SaifNest Native Android App

The production target is a standalone native Android application. The installed app must not require Termux, Python, Flask, Node.js, or a user-managed server.

## Current foundation
- Native Android Media3/ExoPlayer starter under `android-player/`.
- GitHub Actions Android debug APK workflow.
- Native architecture and phased migration documented in [NATIVE-ANDROID-PLAN.md](NATIVE-ANDROID-PLAN.md).
- Premium app mark source at `android-player/app/src/main/res/drawable/saifnest_logo.svg` (vector source of truth). A PNG master design was generated separately; the SVG here is a scalable native project asset.

## Roadmap
1. Build a native Jetpack Compose application shell and branding.
2. Implement sitemap/HTML crawling, pagination, URL normalization, deduplication and persistent scan sessions in Kotlin.
3. Add WebView-based media request observation for JavaScript-heavy sites, with clear limitations.
4. Implement Room-backed download queue, retries, and resume when HTTP Range is supported.
5. Integrate detected media with Media3 playback.
6. Add Bengali/English strings, tests, Android permission/storage review, and signed release setup.

All scanning must respect site access controls and rate limits. The app must not bypass DRM, authentication, paywalls, or CAPTCHA. A detected request is not proof that the media is downloadable.

A phase is complete only after code, tests, CI build evidence, and documented limitations exist. The present player is a starter, not a finished full app.
