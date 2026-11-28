<div align="center">
<img width="1200" height="475" alt="GHBanner" src="https://ai.google.dev/static/site-assets/images/share-ais-513315318.png" />
</div>

# ALT PLAYER

A private, ad-free Android video and audio player for local media.

## Run Locally

**Prerequisites:** [Android Studio](https://developer.android.com/studio)

1. Open Android Studio and choose this project directory.
2. Allow Android Studio to fix any compatibility issues during import.
3. Create a `.env` file in the project directory and set `GEMINI_API_KEY` if your build requires it (see `.env.example`).
4. Remove the release-only debug signing override in `app/build.gradle.kts` when preparing a production build.
5. Run the app on an emulator or physical device.

## Privacy and optional network features

Media scanning, playback, subtitles, the private vault, and playback history remain local to the device. ALT PLAYER does not upload media, filenames, URIs, analytics, or telemetry.

Network access is limited to two explicit app features:

- Release metadata and APK downloads for in-app updates.
- Optional color-only palette synchronization.

The built-in Pine Mint palette and five local color presets remain available without synchronization. Color documents contain no media information and never receive media data.
