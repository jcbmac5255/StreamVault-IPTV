# StreamVault

StreamVault is an Android TV IPTV player built with Kotlin, Jetpack Compose, Room, Hilt, and Media3.

It is designed for large catalogs, remote-friendly navigation, and a polished TV-first playback experience. The app supports common IPTV provider formats, full library browsing, TV playback, and modern Android TV integrations.

## What It Does

- Connects to IPTV providers using `M3U` playlists or `Xtream Codes`
- Syncs and organizes `Live TV`, `Movies`, and `Series`
- Provides a dedicated Android TV experience with Leanback launcher support
- Plays streams with Media3-based playback controls and track selection

## Supported Features

- Provider setup for `M3U` and `Xtream Codes`
- Live TV with categories, channel search, favorites,custom categories and recent channels
- Movies and series libraries with category browsing and search
- Full EPG and now-playing data
- Global search across live channels, movies, and series
- Favorites and custom favorite groups
- Continue watching and playback history
- Multi-view playback for watching multiple streams
- Parental controls with PIN-protected categories
- Subtitle, audio-track, playback-speed, and video-track controls
- Cast integration
- Android TV home integrations such as launcher entry points and TV input support

## Screenshots

Add screenshots to `docs/images/` and update this section when assets are available.

Suggested files:

- `docs/images/dashboard.png`
- `docs/images/live-tv.png`
- `docs/images/player.png`
- `docs/images/movies.png`
- `docs/images/series.png`
- `docs/images/search.png`
- `docs/images/multiview.png`

## Project Structure

- `app/` Android app UI, navigation, DI, Android TV integrations
- `data/` Room database, sync, parsers, repositories, provider implementations
- `domain/` models, repository contracts, use cases, managers
- `player/` playback abstraction and Media3 player implementation

## Build

Requirements:

- Android Studio
- Android SDK
- JDK 17 or compatible Gradle-supported JDK

Useful commands:

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
```

## Notes

- StreamVault is an IPTV client, not a content provider.
- You are responsible for using playlist sources and streams you are authorized to access.
