<div align="center">

# Subtle

**A VLC-style video player for Android — built around subtitles.**

Play anything local. Offline-first. No account, no analytics, no network.

[![Platform](https://img.shields.io/badge/platform-Android-3DDC84?logo=android&logoColor=white)](https://www.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.3-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Engine](https://img.shields.io/badge/engine-libVLC%203.7-FF8800)](https://www.videolan.org/vlc/libvlc.html)
[![minSdk](https://img.shields.io/badge/minSdk-26-blue)](#requirements)

</div>

---

## What is Subtle?

Subtle is a local video player for Android in the spirit of VLC — but with the subtitle
experience treated as the main event rather than a buried menu. It plays the files you
already have, remembers where you stopped, and makes embedded and external subtitles easy
to select and retime.

Everything stays on your device. There is no network permission in the app.

See [`docs/PRD.md`](docs/PRD.md) for the full scope and [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) for the design.

## Features

**Library**
- One-time storage access, then every video on the device is available.
- Home lists all videos; Browse walks the folder tree VLC-style.
- Search by name and sort by name, date added, or last played.
- List and grid views, with thumbnails generated on the fly.
- Optional series grouping (`S01E02` / `1x02`) with expandable headers.
- Resume playback where you left off, shown as a progress bar in the library.
- Finished videos clear their position instead of lingering at 100%.

**Playback**
- Fullscreen player with auto-hiding controls, immersive system bars, and orientation lock.
- Seek bar, ±10 s skips, previous/next video in the folder, playback speed 0.5×–4×.
- Aspect-ratio modes: fit, fill, crop, 16:9, 4:3.
- Tap and drag gestures for scrubbing, brightness, volume, and locking the controls.
- Picture-in-picture from the player button, and automatically when leaving the app
  (Home or Recents) with the player open.
- Background audio through a foreground playback service, with play/pause and previous/next
  controls in the notification.
- Auto-play next episode when one finishes.

**Subtitles**
- Embedded and external subtitle tracks with instant switching.
- Delay adjustment, charset detection and override, and a hard-sub mode.
- Default subtitle language that auto-selects a matching embedded or external track.
- Styling for size, color, outline, and background, with a live preview.
- ASS style override for subtitles that carry their own styling.

**Settings and app**
- Theme (system/light/dark), dynamic color, and in-app language (system/English/Indonesian).
- Force software decoding for chipsets with broken hardware decoding.
- English and Indonesian localization.
- About screen with the libVLC LGPL notice.

See `docs/RELEASE.md` for the release build and the manual QA checklist.

## Tech stack

| Area | Choice |
|---|---|
| Language | Kotlin 2.3, JDK 17 |
| UI | Jetpack Compose, Material 3 |
| Navigation | Navigation 3 |
| DI | Hilt |
| Player engine | libVLC for Android 3.7.x, behind a `PlayerController` contract |
| Persistence | SharedPreferences for settings and playback progress |
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
├── PlaybackService.kt  foreground service for background audio
├── RootViewModel.kt     start gate (onboarding vs main)
├── core/                common/ designsystem/ player/   (engine-agnostic contract)
├── engine/vlc/          libVLC implementation of PlayerController
├── data/                storage/ repository/
├── domain/              model/ repository/ usecase/     (pure Kotlin)
├── di/                  Hilt modules
└── feature/             onboarding/ main/ home/ browse/ player/ settings/ about/ common/
```

## Privacy

Subtle is offline by design:
- No `INTERNET` permission.
- No accounts, analytics, crash reporters, or ads.
- File names and paths are never logged in release builds.
- Your media never leaves the device.

## License and third-party

Subtle bundles **libVLC for Android**, which is licensed under **LGPL-2.1**. libVLC is
dynamically linked as shipped; the app's About screen (Settings → About) carries the LGPL
notice and a link to the libVLC source.

## Documentation

- [`docs/PRD.md`](docs/PRD.md) — scope and behaviour.
- [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) — layers, packages, the `PlayerController` contract.
- [`docs/RELEASE.md`](docs/RELEASE.md) — release build and manual QA checklist.
- [`AGENTS.md`](AGENTS.md) — conventions for working in this repo.
