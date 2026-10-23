# SaifNest Code Audit

Audit date: 2026-10-23  
Scope: current `main` branch, Android native prototype, Gradle configuration, manifest, scanner/player code, and GitHub Actions. This is a source review; no local Android SDK/device runtime is available here, so runtime behavior must still be verified by CI and device tests.

## Executive result

**Status: prototype; not release-ready.** The Android workflow has repeatedly failed before Gradle starts. The latest confirmed cause (run for commit `3f0ffb9`) is a shell pipeline failure during SDK license acceptance: `set -o pipefail` treats `yes` exiting with SIGPIPE after `sdkmanager --licenses` finishes as a job failure. Commit `2efaba2` adjusts this pipeline; its CI run was queued at audit time, so the fix is not yet verified.

## Findings

### P0 — CI fails before compiling
- File: `.github/workflows/ci.yml`
- Evidence: latest failed job stops in “Install required Android SDK packages”; logs show `yes: standard output: Broken pipe` followed by exit code 1.
- Fix applied: temporarily disable pipefail only for the license-acceptance pipeline, then restore it before the SDK package installation command.
- Verification needed: confirm the next workflow reaches `gradle assembleDebug`, then fix any real compile/dependency errors reported there.

### P1 — No APK has been verified
- The repository does not currently contain a verified build artifact from a successful Android CI run.
- The module has Gradle build files but no Gradle wrapper scripts/JAR. CI installs Gradle 8.9 explicitly, so the workflow can build without a wrapper; local build reproducibility would improve by adding a verified wrapper.
- Do not distribute the app until a successful build and install/play smoke test are recorded.

### P1 — Scanner is a limited HTML crawler, not a universal resolver
- File: `android-player/app/src/main/java/bd/saifnest/player/ScannerEngine.java`
- It only reads public HTTP(S) HTML and common media extensions/tags/metadata. It does not inspect browser network requests or execute JavaScript, so many modern video sites will not reveal a stream URL.
- It follows a narrow set of same-host page links; pagination, sitemap discovery, canonical URL handling, and broader link discovery are absent.
- It compares only hostname when deciding same-host links, not scheme/port.
- A direct media URL is fetched using an ordinary request; large media responses may be opened as a body before candidate handling depending on server headers. Use a bounded metadata/Range probe (and always close the response) rather than downloading a whole asset merely to identify it.
- HTML is read fully into memory with `body().string()`; a large response can cause high memory use. Add a response-size limit and content-type checks before parsing.
- Cancellation is a boolean checked between pages. It does not cancel the active OkHttp call, so Stop may wait until network timeout.
- No tests currently exercise URL normalization, redirects, duplicate URLs, MIME handling, or cancellation.

### P1 — Scan lifecycle can show stale or confusing UI state
- File: `android-player/app/src/main/java/bd/saifnest/player/MainActivity.java`
- A single-thread executor serializes scans. Cancelling a scan does not immediately remove its queued work; starting another scan can leave it waiting behind the old network request.
- UI callbacks posted from an older scan can update the screen after a newer scan has started. Use a scan-generation token and ignore callbacks not belonging to the active generation.
- The shared `singleLinkMode` boolean is read inside asynchronous callbacks. Capture the mode at scan start so switching tabs during a scan does not alter its status wording.
- The results screen has playback only; there is no download action, queue, persisted library, selection of multiple items, duplicate suppression across scans, or site-based folder storage.

### P1 — Player lifecycle and compatibility need smoke tests
- File: `android-player/app/src/main/java/bd/saifnest/player/PlayerActivity.java`
- Media3/ExoPlayer playback is a useful starter, but only basic HTTP(S) URLs are supported. Cookies, custom request headers, expiring URL refresh, and some adaptive/segmented stream edge cases are not handled.
- The player is released in `onStop`; verify rotation, background/foreground, back navigation, and external deep-link launches on real devices.
- The manifest exports the player for a custom deep link. The activity validates HTTP(S) scheme, but should also handle malformed percent-encoded URLs safely and present a user-visible error instead of silently finishing.

### P2 — Product and release polish are missing
- The native module README correctly says this is an early foundation.
- App lock/PIN, Room-backed media library, download manager, range-verified parallel downloads, resume support, localization, adaptive launcher icon, and device testing remain unimplemented.
- The requested original transparent PNG logo is not in the repository yet; do not substitute a redrawn SVG.

## Recommended sequence

1. Get the Android CI workflow green and preserve the resulting APK artifact.
2. Add a small unit-testable URL/media utility layer and scanner tests.
3. Fix scan cancellation and stale UI callbacks.
4. Replace full-body probing with bounded media metadata/Range checks and cap HTML response sizes.
5. Add Room persistence and duplicate suppression before implementing multi-item downloads.
6. Implement downloads with verified Range support, safe filenames, cancellation/resume, and a serial fallback for servers without Range.
7. Add player and UI smoke tests, then lock/PIN and release branding assets.

## Security / scope notes

Only inspect publicly accessible URLs and respect site permissions. Do not bypass authentication, DRM, paywalls, CAPTCHA, or other access controls. Treat discovered URLs and page metadata as untrusted input.
