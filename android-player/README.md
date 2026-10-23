# SaifNest Native Player (Media3 / ExoPlayer)

This Android companion listens for the `saifnest://play?url=<encoded-media-url>` deep link and plays public HTTP(S) streams with AndroidX Media3 ExoPlayer.

## Build

Requirements: JDK 17, Android SDK platform 35, Gradle 8.9 (or compatible), and network access to Google Maven/Maven Central.

From this directory:

```sh
gradle assembleDebug
```

GitHub Actions builds the debug APK and stores it as the workflow artifact `saifnest-player-debug-apk`.

## Test

Use an HTTP(S) media URL that you own or are authorized to access:

```sh
adb shell am start -a android.intent.action.VIEW -d 'saifnest://play?url=https%3A%2F%2Fexample.com%2Fsample.mp4'
```

This is a starter player, not a complete native application. Streams requiring cookies, custom request headers, DRM, or expiring signed URL renewal need further integration. Never use it to bypass access controls.
