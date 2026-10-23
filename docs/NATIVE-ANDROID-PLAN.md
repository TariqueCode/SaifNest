# SaifNest Native Android — Product and Engineering Plan

## Product direction

SaifNest is a standalone Android app. The installed app must not require Termux, Python, Flask, Node.js, or a user-managed server. The existing web prototype is reference material, not the final runtime architecture.

Detailed user-visible behavior and acceptance criteria are defined in [Video Library and Download Specification](VIDEO-LIBRARY-DOWNLOAD-SPEC.md).

## Target stack

- Kotlin for app, scanner, networking, and download orchestration.
- Jetpack Compose + Material 3 for the native interface.
- AndroidX Media3 / ExoPlayer for playback.
- Android WebView (Chromium) as an observation fallback for JavaScript-heavy public pages.
- OkHttp + Kotlin Coroutines for HTTP operations and bounded concurrency.
- Jsoup for HTML parsing and link discovery.
- Room for durable inventory, scan sessions, deduplication and download queue metadata.
- DataStore for preferences and app-lock settings.
- WorkManager for deferrable/recoverable work; foreground service/notifications for long user-initiated transfers when required by Android policy.
- GitHub Actions for lint, unit tests, APK assembly and artifact upload.

## Core modules

1. **app** — Compose navigation, dashboard, library, settings, permissions, themes, Bengali/English localization and optional app lock.
2. **scanner** — sitemap discovery, same-host crawl, pagination, URL normalization, deduplication, bounded concurrency, cancellation, pause/resume and progress events.
3. **media-detection** — HTML/media metadata parsing plus WebView/Chromium observation; records MIME type, candidate URL, source page and verification status.
4. **library** — site-based collections, persistent item identity, new/seen/downloaded filters, and opt-in site refresh.
5. **downloads** — durable queue, safe filenames, retry, cancellation, progress, HTTP Range resume and verified parallel byte-range transfers when supported.
6. **player** — Media3 playback, fullscreen/orientation, seek, speed, supported subtitle/audio-track selection and useful error reporting.
7. **data** — Room entities/DAOs and DataStore preferences.
8. **testing** — URL normalization, same-host rules, sitemap/pagination, deduplication, range-download correctness, queue recovery, app-lock and cancellation tests.

## Product requirements

- Single-link mode resolves a pasted page/direct-media URL and displays results incrementally. Each candidate offers Play and Download when supported.
- Website mode reports pages visited, video pages, media candidates, verified direct files, duplicates skipped, failures, and exact/estimated count status separately.
- The user can select one, a chosen number (for example 100), or all eligible items. Discovered videos go to the library first; downloading remains optional.
- A site-based library remembers previously imported/downloaded items. Re-scans prioritize new videos and show skipped duplicate counts; users can explicitly show duplicates or rescan.
- Optional recurring checks can add new public videos to a site's collection. Automatic downloading is a separate opt-in with limits and storage/network controls. Do not promise real-time monitoring if Android has suspended the app.
- Results appear while scanning continues. Pause, resume and cancel must stop or continue real work, not just change labels.
- The download queue survives process death where possible. Resume only when server range/validator behavior is safe. Parallel segments are used only after byte-range support and consistent object length are verified; otherwise use a normal streaming download.
- HLS/DASH manifests use a manifest-aware download path, not raw MP4 byte splitting. Do not bypass authentication, paywalls, DRM, CAPTCHA, signed access controls, or other restrictions.
- Use app-managed storage by default and Android's Storage Access Framework/MediaStore for export.
- Optional PIN/password and biometric app lock must not store plaintext credentials. App lock does not imply exported files are encrypted.
- Do not embed server secrets or private API keys in the APK, and do not claim every website or player is supported.

## Migration strategy

### Phase 1 — Native foundation and reproducible CI
Keep the Media3 starter, native app shell, Gradle dependencies, package structure and GitHub Actions. Confirm a repeatable debug APK build before calling it build-ready.

### Phase 2 — Inventory and scanner
Implement URL validation, direct-file detection, sitemap discovery, same-host crawl, pagination, persistent Room inventory, duplicate suppression, scan controls and tests.

### Phase 3 — JavaScript-heavy site detection
Add WebView-based observation for eligible HTTP(S) requests, correlate candidates with source pages, verify candidates conservatively, and stream results into the library. WebView cannot expose every resource type or guarantee an extractable direct URL.

### Phase 4 — Library and new-item refresh
Add site collections, selected-item workflows, “new only” views, persisted identity, rescan/duplicate controls, and opt-in recurring checks with last-check status.

### Phase 5 — Durable download manager
Add per-item and bulk queues, progress/speed/ETA where meaningful, pause/cancel/retry, restart recovery, HTTP Range resume, guarded parallel range transfers, safe merging, storage controls and notifications.

### Phase 6 — Player, app lock and polish
Integrate library/local playback, fullscreen and playback controls, optional secure app lock, Bengali/English strings, accessibility, error states and device testing.

### Phase 7 — Release readiness
Run lint, unit tests, debug APK build, install smoke tests on real Android devices, storage/privacy review, and signed release configuration. Store signing credentials in GitHub Actions secrets only.

## Definition of done

- Code and relevant tests are committed to GitHub.
- CI and relevant tests pass.
- Limitations and test evidence are recorded.
- A downloadable APK artifact is produced when the build succeeds.
- No feature is described as complete merely because placeholder UI exists.

## Current status (2026-10-23)

The repository contains an early Java-based native UI, a basic same-host HTML/media candidate scanner, and a Media3 player starter. It is a prototype, not the complete product. Durable Room inventory, sitemap/pagination discovery, JavaScript request observation, deduplication across scans, download queue, safe parallel downloads, app lock, and real-device validation remain to be implemented and verified. The original transparent PNG logo must be used as supplied; do not redraw or convert it to SVG.
