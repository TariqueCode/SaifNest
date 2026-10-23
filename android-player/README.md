# SaifNest Native Android

The Android module is the start of a standalone Kotlin/Media3 application. The installed product must not require Termux, Python, Flask, Node.js, or a separately managed server.

## Current scope
- Media3/ExoPlayer playback starter.
- Scalable brand artwork at `app/src/main/res/drawable/saifnest_logo.svg`.
- GitHub Actions debug APK workflow.

## Build
Requirements: JDK 17, Android SDK platform 35, Gradle 8.9 (or compatible), and network access to Google Maven/Maven Central.

From this directory:

```sh
gradle assembleDebug
```

The workflow uploads the debug APK as `saifnest-player-debug-apk` when the build succeeds.

## Roadmap
The complete native app still needs the Compose app shell, Kotlin scanner, live media detection, persistent download manager, localization, and device testing. See `docs/NATIVE-ANDROID-PLAN.md` at repository root.

This player starter handles public HTTP(S) URLs only. Streams requiring cookies, custom headers, DRM, or expiring signed URL renewal need additional integration. Do not use it to bypass access controls.
