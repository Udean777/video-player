# AGENTS.md — Subtle

Subtle is an Android video player (VLC-style) for local files, with first-class soft/hard subtitle support.
Offline-first. No accounts, no analytics, no network features in v1.

Read before working:
- `docs/PRD.md` — what to build and what is out of scope
- `docs/ARCHITECTURE.md` — layers, packages, the `PlayerController` contract, build baseline

## Stack (verified stable on 2026-10-16; source of truth is `gradle/libs.versions.toml`)

| Area | Choice |
|---|---|
| Language | Kotlin 2.4.0, JDK 17 |
| Build | AGP 9.3.x with built-in Kotlin, Gradle 9.5+, KSP 2.3.x, version catalog |
| UI | Jetpack Compose (BOM 2026.09.00 → Compose 1.12), Material 3 |
| Navigation | Navigation 3 (`androidx.navigation3`) |
| DI | Hilt (Dagger 2.59.x) |
| Persistence | Room 3 (`androidx.room3`, KSP only) + DataStore for settings |
| Player engine | libVLC for Android 3.7.x (`libvlc-all`), behind `PlayerController` |
| Files | Direct shared-storage scanning: `MANAGE_EXTERNAL_STORAGE` (API 30+) / `READ_EXTERNAL_STORAGE` (API ≤ 29) |
| SDK | minSdk 26, targetSdk 36, compileSdk 37 |

Rules for dependencies:
- Add libraries only through `gradle/libs.versions.toml`. No hardcoded versions in build files.
- Before adding or bumping anything, look up the latest stable version (Google Maven, Maven Central, Gradle Plugin Portal). Never guess a version, and never use alpha/beta/EAP for production code.
- Ask the user before adding any new dependency that is not in the catalog.

## Commands

```bash
./gradlew :app:assembleDebug            # build debug APK
./gradlew :app:testDebugUnitTest        # unit tests (run before every commit)
./gradlew :app:lintDebug                # Android lint
./gradlew :app:connectedDebugAndroidTest  # instrumented tests (device/emulator required)
./gradlew :app:dependencies --configuration debugRuntimeClasspath   # inspect deps
```

## Project layout

```
app/src/main/java/app/subtle/player/
├── SubtleApp.kt, MainActivity.kt
├── core/        common/ designsystem/ player/ (engine-agnostic contract) 
├── engine/vlc/  libVLC implementation of PlayerController
├── data/        local/ (Room) storage/ (device scan + storage access) preferences/ repository/
├── domain/      model/ repository/ usecase/ — pure Kotlin, no Android imports
├── di/          Hilt modules
├── feature/     onboarding/ home/ browse/ main/ player/ settings/ — each with Route, Screen, ViewModel, UiState
└── navigation/  AppNavHost, NavKey types
```

Dependency direction: `feature → domain ← data`. `feature` and `data` may use `core`. `domain` depends on nothing.
`engine/vlc` is only referenced from `di/`. Nothing else may import `org.videolan.*`.

## Skills (load the matching one before touching that area)

- `android-kotlin-compose` — Kotlin, Compose, ViewModel, Navigation 3, Room 3, Hilt, testing conventions
- `libvlc-playback` — libVLC lifecycle, tracks, soft/hard subtitles, subtitle styling and delay
- `saf-storage` — folder permission, scanning, subtitle discovery, charset normalization (folder-picker parts are superseded by the direct all-files scan)

## Code rules

Always:
- Use the official Kotlin style, 4-space indent, 120-column limit. Prefer `val`, immutable data classes, sealed types for state and errors.
- Keep composables stateless where possible: state down, events up. One `Route` (stateful) + one `Screen` (stateless, previewable) per screen.
- Collect flows in UI with `collectAsStateWithLifecycle()`.
- Inject dispatchers (`@IoDispatcher`, `@DefaultDispatcher`, `@PlayerDispatcher`); never hardcode `Dispatchers.IO` in classes under test.
- Put every user-facing string in `res/values/strings.xml` (English) and `res/values-in/strings.xml` (Indonesian).
- Write a unit test for every new domain function and ViewModel. Prefer fakes over mocks.
- Add `@Preview` for each screen-level composable (light + dark).

Never:
- Use `GlobalScope`, `runBlocking` on the main thread, `!!`, `lateinit` for injectable state, or `Thread.sleep`.
- Catch `CancellationException` without rethrowing.
- Touch libVLC objects from composables or ViewModels directly; go through `PlayerController`.
- Read user media through the app's storage access (`MANAGE_EXTERNAL_STORAGE` on API 30+, `READ_EXTERNAL_STORAGE` on API ≤ 29), requested once from onboarding. "Open with" still arrives as a `content://` URI.
- Add logging of file names or paths to release builds.
- Reproduce or bundle copyrighted media, subtitles, or posters in the repo.

## Workflow

1. For any task touching more than 3 files, state a short plan first and wait for confirmation.
2. Make the smallest change that satisfies the task. Do not refactor unrelated code.
3. Build, run unit tests and lint. Fix everything you introduced.
4. Report: what changed, what you verified, what you could not verify (e.g. needs a physical device).

Definition of done: compiles, `testDebugUnitTest` and `lintDebug` pass, no new warnings, UI state survives rotation and process death where relevant, strings localized, and `docs/` updated if behavior or architecture changed.

## Licensing

libVLC is LGPL-2.1. Keep it dynamically linked as the AAR provides it, expose a license screen (Settings → About) with the LGPL notice and a link to the libVLC source, and do not ship a modified libVLC without publishing the changes.

## Known device caveats (verify on real hardware, not only emulator)

- Hardware decoding differs per chipset; keep the "force software decoding" toggle working.
- HEVC, DTS/EAC3 and PGS subtitles are the first things to test on any engine or version change.
