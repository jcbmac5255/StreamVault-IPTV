# Nexus IPTV

<p align="center">
  <a href="https://github.com/jcbmac5255/Nexus-IPTV/releases/latest/download/Nexus.apk"><img src="https://img.shields.io/badge/Download-Nexus.apk-2ea44f?style=for-the-badge&logo=android" alt="Download Nexus APK" /></a>
  <a href="https://github.com/jcbmac5255/Nexus-IPTV/releases/latest"><img src="https://img.shields.io/github/v/release/jcbmac5255/Nexus-IPTV?display_name=tag&style=for-the-badge&color=0f766e" alt="Latest release" /></a>
  <a href="https://cash.app/$jakem7612"><img src="https://img.shields.io/badge/Cash%20App-%24jakem7612-00C244?style=for-the-badge&logo=cashapp&logoColor=white" alt="Support on Cash App" /></a>
  <a href="https://venmo.com/u/jake7612"><img src="https://img.shields.io/badge/Venmo-%40jake7612-3D95CE?style=for-the-badge&logo=venmo&logoColor=white" alt="Support on Venmo" /></a>
</p>

**Nexus IPTV** is the official Android TV client for the Nexus IPTV service. Sign in with the credentials your reseller provided and the app handles the rest — live channels, movies, series, EPG, recording, and Cast — all in a remote-friendly TV interface.

Built for the living room: D-pad-first navigation, fast channel switching, and a player that feels good from the couch. Phones and tablets are supported, but Android TV is the primary target.

## Quick start

1. **Download** the latest APK: [Nexus.apk](https://github.com/jcbmac5255/Nexus-IPTV/releases/latest/download/Nexus.apk) — or grab it from your Nexus reseller's distribution link.
2. **Install** on your Android TV box (Fire TV, Shield, NVIDIA Shield, Chromecast with Google TV, etc.) by enabling "Install unknown apps" for your file manager / downloader and opening the APK.
3. **Launch** the app — tap **Sign in to Nexus** on the welcome screen.
4. **Enter your username and password** that your reseller sent you. That's it — the server URL is already configured.
5. Wait ~30 seconds while Nexus indexes your channels, movies, and series. Once it lands on the home screen, you're ready to watch.

## Features

### TV-first navigation
- D-pad-friendly focus, navigation, and playback throughout
- Numeric remote input for direct channel entry
- Preview-while-browsing mode
- TV-friendly search and text entry

### Live TV
- Combined-source live channels with favorites and recent channels
- Custom groups and pinned categories for the channels you watch most
- Long-press a channel or category for quick actions (favorite, pin, hide, lock with a PIN, custom group, queue for split-screen)
- Full EPG grid with program search
- Live rewind / timeshift playback (up to 30 minutes) even when the provider doesn't expose catch-up
- Multi-view split-screen for watching multiple channels at once
- Provider archive / catch-up support when available

### Movies and series
- Two VOD layouts: modern shelf-based browsing or classic left-sidebar categories
- Detailed info pages with continue watching and resume
- Long-press categories or custom groups to hide, rename, or reorder
- In-player episode switching for series
- Automatic next-episode playback

### Recording and reminders
- Schedule recordings from the guide or directly from a channel
- Program reminders for when you want a notification but not a recording
- Conflict detection and recording-job repair
- App-managed default recording folder, with optional custom storage selection
- Built-in player for completed recordings, with a live "REC" indicator while a recording is in progress
- Bundled Media3 FFmpeg audio fallback for AC-3, E-AC-3, DTS, MP2, and TrueHD streams

### Parental controls
- Hide categories entirely
- PIN-protect categories so kids can't browse them
- Option to hide locked content from the rest of the app
- Adult-category detection using provider flags and category-name heuristics

### Platform integrations
- Android TV Watch Next integration
- Launcher recommendations and TV Live Channels (TV Input Framework)
- Google Cast sender
- In-app update delivery — newer Nexus releases download and install without leaving the app

## In-app updates

Settings → About → Check for updates. New versions of Nexus are published on GitHub Releases. The app checks once per day on launch (or immediately when you tap "Check for updates") and surfaces a card on the home screen when a new version is ready.

You can also wire a Nextcloud / personal distribution link to the same release flow — point it at [Nexus.apk on GitHub](https://github.com/jcbmac5255/Nexus-IPTV/releases/latest/download/Nexus.apk) and your users always get the latest signed build.

## Support

Sign-in trouble or account issues — contact your reseller.

If Nexus has been useful to you, you can support development here:

- **Cash App**: [$jakem7612](https://cash.app/$jakem7612)
- **Venmo**: [@jake7612](https://venmo.com/u/jake7612)

## Build (developers only)

Requires JDK 17 and Android Studio. The bundled Media3 FFmpeg AAR for audio fallback is checked in — no NDK setup needed unless you're rebuilding it.

```bash
./gradlew assembleDebug                 # debug APK
./gradlew :app:assembleRelease          # signed release (needs keystore.properties)
./gradlew testDebugUnitTest             # all JVM unit tests
```

See [`CLAUDE.md`](CLAUDE.md) for architecture notes if you're contributing or forking.

## Credits

Nexus IPTV is based on the StreamVault open-source codebase. Original architecture, player, and IPTV protocol implementations by [Davidona](https://github.com/Davidona). This fork is maintained by Jake McClanahan and rebranded for the Nexus IPTV service.

## License

Source-Available License (Non-Commercial). See [LICENSE](LICENSE). Commercial use requires explicit permission.
