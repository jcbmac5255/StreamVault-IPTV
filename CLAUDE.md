# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

StreamVault is a TV-first IPTV player for Android TV (also runs on phones/tablets) built with Kotlin, Jetpack Compose, Compose-for-TV, Room, Hilt, Media3, and WorkManager. It supports `M3U`, `Xtream Codes`, and `Stalker Portal` providers with separate Live TV / Movies / Series flows, EPG, DVR, Cast, and a Messenger-based plugin API.

## Build & test commands

JDK 17 is required. NDK is only needed if rebuilding the bundled Media3 FFmpeg AAR.

```bash
./gradlew assembleDebug                 # debug APK
./gradlew :app:assembleRelease          # release APK (signed if keystore.properties exists)
./gradlew assembleBeta                  # beta build type (release-like, .beta applicationId suffix)
./gradlew testDebugUnitTest             # all JVM unit tests across modules
./gradlew :data:testDebugUnitTest       # single-module tests
./gradlew :data:testDebugUnitTest --tests "com.nexus.iptv.data.sync.SyncManagerTest"   # single test class
./gradlew :data:connectedAndroidTest    # Room migration / instrumentation tests
./gradlew koverXmlReportCi koverHtmlReportCi   # coverage; report at build/reports/kover/
```

CI (`.github/workflows/release.yml`) runs `testDebugUnitTest koverXmlReportCi koverHtmlReportCi` then `:app:assembleRelease`. Releases publish only on manual `workflow_dispatch`.

## Architecture

Four Gradle modules with strict layering — `:app` → `:data` + `:player`, both → `:domain`. `:domain` has no Android UI deps and defines all repository/manager contracts.

- **`:domain`** — pure contracts and models. `repository/` interfaces, `manager/` interfaces (Backup, ParentalControl, Recording, Reminder, DriveBackupSync, ProviderSetupInputValidator, ProviderSyncStateReader), `provider/IptvProvider` (the abstraction every backend implements), `usecase/` (Validate/AddProvider, SyncProvider, GetRecommendations, ExportBackup, ImportBackup, ScheduleRecording, UnlockParentalCategory, …), and `model/` (Channel, Movie, Series, Program, Provider, RecordingModels, EpgModels, PlaybackHistory, SyncState, …).

- **`:data`** — implementations and persistence.
  - `remote/xtream/`, `remote/stalker/` — `IptvProvider` implementations via Retrofit/OkHttp. `XtreamProvider` and `StalkerProvider` are the two non-M3U backends.
  - `parser/` — M3U / XMLTV / Xtream JSON parsing. JVM tests use `kxml2` for XmlPullParser since the JVM has no built-in impl.
  - `local/StreamVaultDatabase.kt` — Room database (currently **version 52**, `exportSchema = true`). Schemas are tracked under `data/schemas/com.nexus.iptv.data.local.StreamVaultDatabase/`. **Any entity change requires a Migration and a new schema JSON committed alongside the code.**
  - `sync/` — orchestration for catalog refresh. `SyncManager` is the entry point; it splits into `SyncManagerXtreamFetcher` / `XtreamLiveStrategy` / `XtreamSupport` / `CatalogStager` / `M3uImporter` files. `ProviderSyncWorker`, `XtreamIndexWorker`, `BackgroundEpgSyncWorker` are WorkManager jobs scheduled from `StreamVaultApp.onCreate`. Live, VOD, and EPG can sync independently.
  - `manager/recording/` and `manager/reminder/` — DVR and program reminders, including conflict detection and reconcile worker.
  - `repository/` — single `*RepositoryImpl` per domain repository contract; wired in `app/di/RepositoryModule.kt`.
  - `preferences/`, `security/` — DataStore preferences plus `AndroidKeystoreCredentialCrypto` for provider credentials.

- **`:player`** — Media3/ExoPlayer abstraction. Subpackages: `playback/`, `audio/` (FFmpeg fallback for AC-3/E-AC-3/DTS/MP2/TrueHD via the bundled `media3-decoder-ffmpeg-1.9.2.aar` in `player/libs/`), `timeshift/` (up to ~30 min live rewind buffer), `tracks/`, `stats/`, `ui/`. The FFmpeg AAR is consumed via `implementation(files(...))` in `app/build.gradle.kts` — do not remove or relocate that file without updating the audio fallback path.

- **`:app`** — Hilt entry point (`StreamVaultApp`, `MainActivity`), Compose UI, navigation, TV integrations.
  - `ui/screens/` is organized by feature (home, dashboard, live/epg, movies, series, vod, multiview, player, provider, settings, search, welcome, plugins, favorites).
  - `di/` — Hilt modules (`DatabaseModule`, `NetworkModule`, `RepositoryModule`, `PlayerEngineQualifiers`, `SlowQueryLoggingOpenHelperFactory`).
  - `tvinput/` — Android TV Input Framework service (`StreamVaultTvInputService`) and channel sync; `tv/` — Watch Next + launcher recommendations.
  - `cast/` — Google Cast sender + route chooser.
  - `plugins/` — host side of the plugin API (see `docs/PLUGIN_API.md`); plugins are separate APKs bound via `Messenger` IPC with action `com.nexus.iptv.plugin.API` (declared in both the app `<queries>` and the plugin's exported service).
  - `update/` — in-app update via `GitHubReleaseChecker` + `AppUpdateInstaller`.

## Build types and signing

- `debug`, `beta`, `release` are defined in `app/build.gradle.kts`.
- `beta` does `initWith(release)`, adds `applicationIdSuffix=".beta"` and `versionNameSuffix="-beta"`, sets `APP_UPDATE_CHANNEL="beta"`, and is intentionally not minified for faster CI distribution.
- `release` enables `isMinifyEnabled` and `isShrinkResources` with `proguard-rules.pro`.
- `signingConfigs.release` is **only created if `keystore.properties` exists** at repo root (gitignored). Both `release` and `beta` reference it conditionally — builds work without it (unsigned). CI decodes `RELEASE_KEYSTORE_BASE64` into `keystore/release.jks` and writes `keystore.properties` at build time.
- `app/build.gradle.kts` computes `OFFICIAL_SIGNING_CERT_SHA256` from the keystore at configure time and bakes it into `BuildConfig`; `util/OfficialBuildVerifier.kt` uses this for integrity checks. If you change signing, this constant changes.

## Dev seeding (debug only)

`local.properties` keys `xtream.dev.*` / `m3u.dev.*` are read by `app/build.gradle.kts` and exposed as `BuildConfig.XTREAM_DEV_*` / `BuildConfig.M3U_DEV_*` **in the `debug` build type only**. Release inherits the empty defaults from `defaultConfig`, so a release APK can never ship contributor credentials. `WelcomeViewModel.maybeSeedDevProvider()` consumes these when no provider exists. See `docs/DEV_SEEDING.md`.

## Conventions worth knowing

- **Room migrations**: bumping `version` in `StreamVaultDatabase` requires both a `Migration` object and a checked-in schema JSON under `data/schemas/`. `exportSchema = true` is intentional.
- **Provider abstraction**: when adding a backend, implement `domain/provider/IptvProvider` and wire it through the relevant sync strategy files under `data/sync/`. Do not branch on provider type inside repositories.
- **Compose TV**: `compose-tv-material` is pinned at `1.0.1` — note in `gradle/libs.versions.toml` warns to retest TV navigation before bumping. Remaining experimental TV Material usage is limited to `FilterChip` in `SearchScreen`.
- **Background work** is initialized in `StreamVaultApp.onCreate` (Hilt-injected). New periodic workers should be registered there with `ExistingPeriodicWorkPolicy` and network/idle constraints to match existing patterns.
- **WorkManager + Hilt**: there is a Gradle workaround at the bottom of `app/build.gradle.kts` that disables `hiltJavaCompileDebugUnitTest`. Leave it in place.
- **Plugin API stability**: the `Messenger` protocol is the public contract for third-party plugins — see `docs/PLUGIN_API.md`. Treat `StreamVaultPluginContract.kt` as load-bearing.

## Graphify

A Graphify knowledge graph may exist at `graphify-out/` (gitignored). When present:
- Read `graphify-out/GRAPH_REPORT.md` before answering architecture/codebase questions.
- If `graphify-out/wiki/index.md` exists, navigate that instead of raw files.
- For cross-module "how does X relate to Y", prefer `graphify query`, `graphify path`, or `graphify explain` over grep.
- Run `graphify update .` after modifying source to keep the graph current (AST-only, no API cost).

## Reference

- `docs/CHANGELOG.md` — release notes (consumed by the release workflow)
- `docs/PLUGIN_API.md` — plugin protocol
- `docs/FFMPEG.md`, `docs/FFMPEG-LGPL-NOTICE.md` — bundled audio decoder details and LGPL compliance notes
- `docs/DEV_SEEDING.md` — `local.properties` seeding
- `docs/GOOGLE_DRIVE_SETUP.md` — Drive backup sync setup

## License

StreamVault Source-Available License (Non-Commercial), effective April 2026. See `LICENSE`. This is not OSI-approved open source; commercial use requires permission.
