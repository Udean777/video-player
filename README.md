<div align="center">

# Subtle

**A VLC-style video player for Android — built around subtitles.**

Play anything local. Offline-first. No account, no analytics, no network.

[![Platform](https://img.shields.io/badge/platform-Android-3DDC84?logo=android&logoColor=white)](https://www.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.3-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Engine](https://img.shields.io/badge/engine-libVLC%203.7-FF8800)](https://www.videolan.org/vlc/libvlc.html)
[![minSdk](https://img.shields.io/badge/minSdk-26-blue)](#requirements)
[![Status](https://img.shields.io/badge/status-early%20development-orange)](#status)

</div>

---

## What is Subtle?

Subtle is a local video player for Android in the spirit of VLC — but with the subtitle
experience treated as the main event rather than a buried menu. It plays the files you
already have, remembers where you stopped, and (soon) makes embedded and external
subtitles easy to select, retime and style.

Everything stays on your device. There is no network permission in the app.

## Status

Early development. The foundation and library browsing work; subtitles are next.

| Area | State |
|---|---|
| Storage access + onboarding | ✅ |
| Home (all videos) & Browse (folder tree) | ✅ |
| List / grid with thumbnails | ✅ |
| Fullscreen player (play, pause, seek, ±10 s, prev/next) | ✅ |
| Resume + watched progress | ✅ |
| Subtitles (embedded, external, styling) | 🚧 planned |
| Settings, gesture layer, PiP, audio tracks | 🚧 planned |

See [`docs/PRD.md`](docs/PRD.md) for the full scope and [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) for the design.

## Features

**Working now**
- One-time storage access, then browse every video on the device.
- Home lists all videos; Browse walks the folder tree VLC-style.
- List and grid views, thumbnails generated on the fly.
- Fullscreen player with auto-hiding controls, immersive system bars, and orientation lock.
- Seek bar, skip controls for ±10 s, previous/next video within the folder.
- Resume playback where you left off, with a progress bar in the library.
- Finished videos clear their position instead of lingering at 100%.

**Roadmap**
- Embedded and external subtitle tracks, charset detection, delay and styling.
- Hard-subtitle mode for releases with burned-in subs.
- Gestures (brightness, volume, seek), audio track selection, playback speed.
- Picture-in-picture, settings, and full English/Indonesian localization.

## Tech stack

| Area | Choice |
|---|---|
| Language | Kotlin 2.3, JDK 17 |
| UI | Jetpack Compose, Material 3 |
| Navigation | Navigation 3 |
| DI | Hilt |
| Player engine | libVLC for Android 3.7.x, behind a `PlayerController` contract |
| Persistence | SharedPreferences today, Room 3 planned |
| Build | AGP 9.4, Gradle 9.5, version catalog |
| SDK | minSdk 26 · targetSdk 36 · compileSdk 37 |

## Architecture

Single module, package-by-layer, with the engine kept behind an interface so it can be
swapped without touching the UI.

```
feature/*   Compose UI + ViewModel
    │ depends on
    ▼
domain/     models, repository interfaces, use cases — pure Kotlin
    ▲ implemented by
    │
data/*      storage scan, repository implementations

core/player  PlayerController contract (engine-agnostic)
engine/vlc   libVLC implementation — wired in only from di/
```

Rules that keep it honest:
- `domain` has no Android or libVLC imports.
- Only `engine/vlc` imports `org.videolan.*`, and only `di/` wires the engine in.
- Unidirectional data flow: UI emits actions → ViewModel updates an immutable `UiState` → UI renders.

Full details in [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md).

## Requirements

- Android Studio (latest stable) with **JDK 17**.
- Android SDK with **compileSdk 37** and the matching build tools.
- Android 8.0 (API 26) or newer on a device or emulator.

## Getting started

```bash
git clone <repo-url>            # clone
cd Subtle
./gradlew :app:assembleDebug    # build the debug APK
```

Install on a device/emulator:

```bash
./gradlew :app:installDebug
```

## Build, test and lint

```bash
./gradlew :app:assembleDebug            # debug APK
./gradlew :app:testDebugUnitTest        # unit tests
./gradlew :app:lintDebug                # Android lint
./gradlew :app:connectedDebugAndroidTest  # instrumented tests (device required)
```

## Project layout

```
app/src/main/java/app/subtle/player/
├── MainActivity.kt      single activity; hosts onboarding / main / player
├── RootViewModel.kt     start gate (onboarding vs main)
├── core/                common/ designsystem/ player/   (engine-agnostic contract)
├── engine/vlc/          libVLC implementation of PlayerController
├── data/                storage/ repository/            (local/ + preferences/ planned)
├── domain/              model/ repository/              (pure Kotlin; usecase/ planned)
├── di/                  Hilt modules
└── feature/             onboarding/ main/ home/ browse/ player/ common/
```

## Privacy

Subtle is offline by design:
- No `INTERNET` permission in v1.
- No accounts, analytics, crash reporters, or ads.
- File names and paths are never logged in release builds.
- Your media never leaves the device.

## License and third-party

The app is under development and its distribution license is not final.
It bundles **libVLC for Android**, which is licensed under **LGPL-2.1**. libVLC is used
dynamically linked as shipped; the license screen (Settings → About, planned) will carry
the LGPL notice and a link to the libVLC source.

## Documentation

- [`docs/PRD.md`](docs/PRD.md) — what to build and what is out of scope.
- [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) — layers, packages, the `PlayerController` contract.
- [`AGENTS.md`](AGENTS.md) — conventions for working in this repo.
