# SaifNest

<p align="center">
  <img src="android-player/app/src/main/res/drawable-nodpi/saifnest_logo.png" alt="SaifNest logo" width="180" />
</p>

<p align="center"><strong>A modern Android browser with built-in media discovery.</strong></p>

<p align="center">
  <img alt="Platform" src="https://img.shields.io/badge/platform-Android-3DDC84?logo=android&logoColor=white" />
  <img alt="Language" src="https://img.shields.io/badge/language-Java%20%2B%20Android-3B82F6" />
  <img alt="Version" src="https://img.shields.io/badge/version-0.2.0-06B6D4" />
  <img alt="Status" src="https://img.shields.io/badge/status-in%20development-orange" />
</p>

SaifNest is an Android-first browser project focused on a clean browsing experience, privacy-conscious content filtering, and convenient discovery of media requests made by pages. The interface uses a deep navy, electric blue, cyan, and silver visual language.

> **Project status:** Active development. The current branch contains an early browser UI and media-detection/player foundation. It is not yet a production-ready browser. Build and device testing must pass before a release can be considered complete.

## Brand identity

The SaifNest mark combines a connected globe with a flowing metallic-blue ribbon. It represents connected browsing, speed, and a single home for web content.

- **Primary colours:** midnight navy, royal blue, cyan, and silver.
- **Logo usage:** keep the mark on transparent backgrounds; do not add a solid background to the master asset.
- **Master logo:** `android-player/app/src/main/res/drawable-nodpi/saifnest_logo.png` is the canonical, user-provided PNG artwork. Do not redraw it as SVG or substitute a generated vector.
- **Android launcher:** adaptive launcher icons reference the canonical `saifnest_logo` drawable resource; Android resolves the PNG from `drawable-nodpi`.
- **App name:** SaifNest.

## Current features

- Native Android application shell with a redesigned browser home screen.
- Address/search bar for entering URLs or search terms.
- WebView-based page rendering and browser navigation.
- Quick links for commonly used sites.
- Browser tools menu with page reload, home, share/copy URL, find-in-page, desktop-site toggle, cache clearing, and an ad-shield toggle.
- Heuristic filtering of selected advertising/tracking hosts and paths, popup windows, and some redirect patterns.
- Media request classification for common video, audio, HLS, and DASH URL patterns.
- Media library screen for discovered candidates.
- Persistent local bookmarks and browsing history.
- Media3/ExoPlayer foundation for compatible media playback.

Feature availability depends on the website, Android/WebView version, and media delivery method. Media detection is best-effort: it does not guarantee that an item can be played or downloaded.

## Technology

- Java and Android SDK
- Android WebView
- AndroidX Media3 / ExoPlayer (HLS and DASH modules included)
- OkHttp
- Jsoup
- GitHub Actions workflow support (where configured)

## Repository layout

```text
SaifNest/
├── README.md
├── NATIVE-ANDROID-PLAN.md
└── android-player/
    └── app/
        └── src/main/
            ├── AndroidManifest.xml
            ├── java/bd/saifnest/player/
            │   ├── BrowserActivity.java
            │   ├── MediaDetectionEngine.java
            │   ├── MediaLibraryActivity.java
            │   ├── PlayerActivity.java
            │   └── SharedMediaStore.java
            └── res/
                ├── drawable/
                └── mipmap*/
```

## Development setup

1. Install a compatible JDK and Android SDK.
2. Open the `android-player/` project in Android Studio, or use the repository's configured Gradle wrapper if available.
3. Allow Gradle to resolve the Android Gradle Plugin and dependencies.
4. Build the debug variant and install it on a test device.
5. Test navigation, permissions, media playback, and filtering against sites you are authorized to access.

Typical Gradle commands (run from the Android project root when the wrapper is present):

```bash
./gradlew assembleDebug
./gradlew test
```

On Windows, use `gradlew.bat assembleDebug` and `gradlew.bat test`.

## Roadmap

- [ ] Verify a clean Android build in CI and on a physical device.
- [ ] Replace the placeholder tab dialog with real multi-tab management.
- [x] Add persistent bookmarks and browsing history (basic local persistence and open-from-list flows).
- [ ] Add editable bookmark management, search/filter, and richer history controls.
- [ ] Build a robust download queue with progress, retry, and resume support when the server supports HTTP Range.
- [ ] Improve media classification using response MIME types and page context.
- [ ] Add settings for privacy, downloads, appearance, and default search.
- [ ] Add automated tests for URL normalization, media classification, and filtering rules.
- [ ] Review accessibility, translations (Bengali/English), storage behavior, and Android version compatibility.
- [ ] Prepare signing, release notes, privacy disclosure, and release build verification.

## Privacy, safety, and limitations

- The browser loads third-party websites; those websites may collect data under their own policies.
- Filtering is heuristic and cannot guarantee that all ads, trackers, popups, or anti-adblock scripts will be blocked.
- WebView behavior differs from full desktop browsers. Some sites may require unsupported APIs, browser extensions, DRM, or specific codecs.
- DRM-protected streams, authentication controls, paywalls, CAPTCHA, and other access restrictions are not bypassed.
- Only access, play, or download content when you have permission and the site's terms allow it.
- A media URL appearing in the library is a detected candidate, not proof of a downloadable file.
- Do not put passwords, signing keys, API secrets, or personal tokens in source control.

## Contributing

Work on the active development branch, keep changes focused, and include build/test evidence with feature changes. Document any known limitations instead of describing incomplete functionality as finished.

## License

No license has been declared for this repository yet. Until a license is added, all rights remain with the copyright holder; do not assume the code is open for reuse.
