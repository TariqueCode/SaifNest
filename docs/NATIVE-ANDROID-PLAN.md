# SaifNest Native Android — Product and Engineering Plan

## Product direction

SaifNest is moving from a browser-based/Python prototype to a standalone Android application. The installed app must not require Termux, a separately installed Python runtime, Flask, Node.js, or a user-managed server.

The existing web prototype and its proven extraction behavior are reference material for feature parity, not the runtime architecture for the final Android product.

## Target stack

- Kotlin for application, scanner, networking, and download orchestration.
- Jetpack Compose + Material 3 for the native interface.
- AndroidX Media3 / ExoPlayer for playback.
- Android WebView (Chromium) as a site-rendering and request-observation fallback for sites that require JavaScript.
- OkHttp + Kotlin Coroutines for HTTP operations and concurrency.
- Jsoup for HTML parsing and link discovery.
- Room for durable inventory, scan sessions, and download queue metadata.
- DataStore for preferences.
- WorkManager for deferrable/recoverable work; a user-visible foreground service is required for long-running user-initiated transfers where Android policy requires it.
- GitHub Actions for lint, unit tests, and APK assembly.

## Core modules

1. **app** — Compose navigation, dashboard, settings, permissions, theme and localization.
2. **scanner** — sitemap discovery, same-host crawl, pagination strategies, URL normalization, deduplication, bounded concurrency, cancellation, pause/resume, and progress events.
3. **media-detection** — page parsing plus WebView/Chromium request observation for media candidates; captures MIME type, URL, referer context when available, and detection source.
4. **downloads** — queue, retry policy, resumable HTTP downloads when servers support byte ranges, progress, storage destination, and failure reasons.
5. **player** — Media3 playback, fullscreen/orientation, seek, speed, subtitle/audio-track selection when supported, and playback error reporting.
6. **data** — Room entities/DAOs and DataStore preferences.
7. **testing** — unit tests for URL normalization, same-host rules, pagination, deduplication, queue transitions, and scanner cancellation.

## Product requirements

- Results appear incrementally; users can open or queue a detected item while a scan continues.
- The scanner must have configurable page limits and concurrency, with visible estimated versus exact counts.
- Pause, resume, and cancel must be real operations, not just UI states.
- Downloads must survive process death where the Android platform and remote server allow it; unsupported resume must restart safely and be reported honestly.
- Use app-private storage by default and Android's Storage Access Framework/MediaStore for user-selected destinations.
- Keep all scanning within user-selected sites and normal public access. Do not bypass authentication, paywalls, DRM, CAPTCHA, or other access controls. Respect site policies and rate limits.
- Never promise that every website, encrypted stream, DRM-protected item, or custom player can be extracted or downloaded.
- Do not embed server secrets or private API keys in the APK.

## Migration strategy

### Phase 1 — Buildable native foundation
Keep the current Media3 player starter, add the native app shell, Gradle dependencies, package structure, CI build and artifact upload. Establish a repeatable debug APK build before adding more modules.

### Phase 2 — Local inventory and native scanning
Implement URL validation, sitemap and HTML discovery, pagination strategies, deduplication, Room persistence, scan controls, and unit tests.

### Phase 3 — Live media detection
Add the WebView-based observation layer for JavaScript-heavy sites, normalize media candidates, and stream results to the UI while scanning. Do not treat a request URL as proof that it is downloadable.

### Phase 4 — Download manager
Add a persistent queue, safe file naming, progress, retry, cancellation, resume support when HTTP Range is available, and foreground notification behavior appropriate to the Android version.

### Phase 5 — Playback and polish
Integrate detected URLs with Media3, improve player controls, add Bengali/English strings, accessibility, empty/error/loading states, and device testing.

### Phase 6 — Release readiness
Run lint, unit tests, debug APK build, install smoke tests on real Android devices, permission/storage review, privacy review, and signed release configuration. Release signing credentials must be stored in GitHub Actions secrets and never committed.

## Definition of done for each phase

- Code is committed to GitHub.
- CI build and relevant tests pass.
- Limitations and test evidence are recorded.
- A downloadable APK artifact is produced when the build succeeds.
- No feature is described as complete merely because placeholder UI exists.

## Current status

The repository currently contains an initial Media3 player starter and a GitHub Actions build workflow. This is not yet the complete native SaifNest application. The previous Python/Flask project source must be treated as a behavior reference and migrated module by module; the native Android app should not launch or depend on that web server.
