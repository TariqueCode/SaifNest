# SaifNest — Video Library, Discovery and Download Specification

## 1. Product experience

SaifNest is a standalone Android application. It must not require Termux, Python, Flask, Node.js, or a separately managed server after installation.

### A. Single-link mode
1. The user pastes a video/page URL.
2. SaifNest validates the URL and begins resolving it immediately.
3. It shows discovery events and candidate media in a live results list; the user does not need to wait for the whole scan to finish.
4. Each result includes a best-known title, source page, media type, detection method, availability/verification status, and download/play actions.
5. The user can play a playable result in the built-in Media3 player or add it to the download queue with one tap.
6. If only a page URL is found, SaifNest may inspect public HTML and, where suitable, render the page in Android WebView and observe eligible network requests. A discovered request is a candidate, not proof that the URL is a complete downloadable file.

### B. Website-library mode
1. The user enters a website/domain and starts an analysis.
2. SaifNest reports scan scope and progress: pages visited, candidate video pages, playable media candidates, verified direct files, skipped duplicates, errors, and whether counts are exact or estimates.
3. The user chooses a bounded target (for example, 1, 10, 100, or all discovered items) and can select/deselect individual results.
4. Discovered videos enter the library first, with Play and Download actions. Downloads are optional and separate from discovery.
5. Results should appear while the crawl continues, not only at completion.
6. The user can filter to new items, already downloaded items, failed items, or all items.

### C. New-video detection and website folders
- Organize a library by site/host, with each site's metadata and downloads grouped under that site.
- Persist canonicalized source-page URLs and media fingerprints in Room so previously imported/downloaded items are not repeatedly added.
- A scan should prioritize new items and clearly report duplicates skipped. Do not treat URL equality alone as perfect identity: query tokens may rotate, and two different pages can point to the same media.
- Offer an explicit “Check for new videos” action and an opt-in recurring check for a site. When a check discovers new public items, add them to that site's library and notify the user. Automatic downloading must be a separate opt-in setting with limits, Wi-Fi/storage controls, and an easy stop switch.
- A site cannot be monitored while the app is force-stopped or Android prevents its work from running. Background checks must use Android-approved scheduling and clearly show last-check/next-check status; do not promise instant updates from every site.

## 2. Discovery and URL resolution

Use a layered strategy, ordered from fast/cheap to more expensive:
1. Validate and normalize the supplied URL.
2. Check the supplied URL's response headers and content type when allowed.
3. Parse HTML metadata and media elements; inspect public links and known video-page patterns.
4. Discover sitemap.xml / sitemap index files and common public video sitemap entries.
5. Crawl same-host pages with bounded concurrency, depth, page count, request timeout, and per-host rate limits. Support pagination and “load more” links where discoverable.
6. For JavaScript-heavy pages, optionally load the page in Android WebView and observe eligible HTTP(S) resource requests. Correlate candidates with the page and response metadata where possible.
7. Verify a candidate with a lightweight request (HEAD when supported, otherwise a small ranged GET); record verification as unknown when the server blocks probing or the method is unsupported.
8. Expose the best available source/page URL even when a direct file URL cannot be resolved.

Do not bypass authentication, paywalls, DRM, CAPTCHA, signed access controls, or other restrictions. Do not claim all websites are supported. WebView observation does not expose every possible resource: blob URLs, encrypted/DRM playback, service-worker behavior, custom protocols, and some player implementations need different handling or may be unsupported.

## 3. Duplicate suppression and library identity

- Persist discovered pages, media candidates, download jobs, completion state, and file fingerprints locally.
- Normalize URLs conservatively: remove fragments and normalize host/default ports, but preserve query parameters unless a proven safe rule says otherwise. Query strings can contain required signatures or identify different media.
- Use a composite identity where available: canonical source page + media URL + stable metadata (duration, size, ETag/Last-Modified, or content hash when safely available).
- Before listing an item as new, compare it with stored library entries and completed downloads. Mark duplicates as “Already in library” or “Already downloaded”; do not silently erase scan counts.
- Provide “Show duplicates” and “Rescan anyway” controls for users who need them.
- Never claim two files are identical solely because their titles match.

## 4. Download manager

- Downloads use a durable Room-backed queue with queued, resolving, downloading, paused, retrying, completed, skipped, and failed states.
- Support per-item and bulk selection, pause/cancel/retry, overall and per-file progress, speed, bytes transferred, ETA when meaningful, and readable failure reasons.
- Resume a partial file only when the server supports byte ranges and validators/response semantics are consistent. If resume is unsupported or unsafe, restart from zero and explain why.
- Segment/parallel downloads are an optimization, not a universal guarantee. Only split a single file into byte ranges when the server advertises or demonstrates Accept-Ranges: bytes, responds correctly to range requests, and provides consistent file identity/length. Use bounded parallelism, validate every segment, and merge in exact byte order. If these checks fail, use a normal streaming download.
- Do not split HLS/DASH playlists as if they were one MP4. HLS/DASH require a manifest-aware Media3-compatible download path; encrypted or DRM-protected streams must not be bypassed. Keep segmented transfer concurrency configurable and conservative to avoid overloading sites.
- Use safe filenames and prevent path traversal. Default to app-managed storage, with user-selected export destinations through Android's Storage Access Framework/MediaStore.
- Persist enough queue state to recover after process death or device restart where Android permits. Show storage requirements and handle low-space failures safely.

## 5. Playback

- Use AndroidX Media3/ExoPlayer for supported direct files and compatible HLS/DASH streams.
- Each library item should have a one-tap Play action. Support fullscreen, seek, playback speed, orientation, and supported audio/subtitle track selection as the player matures.
- Show a useful error when the source needs cookies/headers, is expired, is DRM protected, is an unsupported codec/container, or is not directly playable. Do not mislabel an inaccessible URL as a working video.
- Local completed downloads should remain playable offline when the format is supported.

## 6. App protection

- Offer an optional app lock with a user-defined PIN/password, confirm-on-setup, lock-on-resume/timeout, and a recovery path that does not expose stored credentials.
- Store only a salted, slow password/PIN verifier using Android Keystore-backed protection where appropriate; never store the password in plaintext or commit it to the repository.
- Support biometric unlock when available, but keep a secure fallback. Avoid logging secrets or sensitive URLs. Explain that app lock protects the app UI; it does not encrypt exported media files unless file encryption is separately implemented.

## 7. Performance and safety limits

- Show results as soon as discovered; never wait for the entire domain crawl before rendering the list.
- Use bounded concurrency, per-host pacing, cancellation, pause/resume, connection/read timeouts, and a user-configurable maximum pages/items.
- Start with conservative defaults and adapt only based on reliable response data. Do not send hundreds of parallel requests to one host.
- Respect robots/site policies where applicable, legal access, copyright, and the site's normal public access controls.
- Keep scan, direct-link verification, playback, and downloading as distinct states so the interface never promises more than has been verified.

## 8. Acceptance tests

- Pasting a public direct MP4 URL produces a candidate quickly, verifies it when possible, and offers Play/Download.
- Pasting a public HTML video page produces a live result if a supported direct URL can be discovered; otherwise the source page remains available with an honest status.
- A site scan reports pages visited, discovered video pages, direct media candidates, duplicates skipped, and errors separately.
- A 100-item selection queues no more than the 100 selected eligible items; individual and bulk selection both work.
- A second scan skips previously downloaded items and reports how many were skipped.
- A completed item is not duplicated by repeated scans; “rescan anyway” remains available.
- Parallel range download is used only after range support and consistent total length are verified; otherwise the app falls back to a single stream.
- Pausing/cancelling stops network work, and interrupted jobs recover safely.
- App lock can be enabled/disabled securely and never stores plaintext credentials.
- GitHub Actions builds and tests the app before an APK is described as build-ready.
