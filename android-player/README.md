# SaifNest Native Android

The Android module is the start of a standalone native application. The installed product must not require Termux, Python, Flask, Node.js, or a user-managed server.

## Current scope
- Native Android launch screen with HTTP(S) media-link entry.
- Media3/ExoPlayer playback starter.
- Scalable brand artwork in `app/src/main/assets/saifnest_logo.svg`.
- GitHub Actions debug APK workflow.

## Build
Requirements: JDK 17, Android SDK platform 35, Gradle 8.9 (or compatible), and network access to Google Maven/Maven Central.

From this directory:

```sh
gradle assembleDebug
```

The workflow uploads the debug APK as `saifnest-player-debug-apk` when the build succeeds.

## Remaining work
Website scanning, sitemap/pagination crawling, live media request observation, a persistent download queue, pause/resume, localization, launcher bitmap/adaptive icons, and device testing are not implemented yet. See `../docs/NATIVE-ANDROID-PLAN.md`.

This is an early native foundation, not a complete app. Playback supports public HTTP(S) URLs; streams requiring cookies, custom headers, DRM, or expiring signed URL renewal need additional integration. Do not use it to bypass access controls.
