# Architecture — Subtle

Single Gradle module (`:app`), package-by-layer with feature packages. Split into modules later only when build time or team size justifies it.

## 1. Layers

```
feature/*  (Compose UI + ViewModel)
    │ depends on
    ▼
domain/    (models, repository interfaces, use cases — pure Kotlin)
    ▲ implemented by
    │
data/*     (Room, SAF scanner, DataStore, repository implementations)

core/player   PlayerController contract (engine-agnostic)
engine/vlc    libVLC implementation, wired in only by di/
```

Rules
- `domain` has no Android or libVLC imports.
- `feature` talks to `domain` (use cases, repositories) and to `core/player` (`PlayerController`). Never to `data` or `engine`.
- `engine/vlc` is the only package that imports `org.videolan.*`.
- Unidirectional data flow: UI emits actions → ViewModel updates immutable `UiState` → UI renders.

## 2. Package structure

```
app/src/main/java/app/subtle/player/
├── SubtleApp.kt                       @HiltAndroidApp
├── MainActivity.kt                    single activity, enableEdgeToEdge(), hosts AppNavHost
├── RootViewModel.kt                   start gate: onboarding vs main tabs, based on storage access
├── PlaybackService.kt                 foreground service: keeps playback alive, notification controls
├── navigation/
│   ├── AppNavHost.kt                  NavDisplay + entry provider
│   └── Keys.kt                        @Serializable NavKey types
├── core/
│   ├── common/                        dispatcher qualifiers, AppError, small extensions
│   ├── designsystem/                  SubtleTheme, typography, shared components
│   └── player/                        PlayerController, PlayerState, Track, PlayerError, MediaSource, PlaybackHolder
├── engine/vlc/
│   ├── VlcPlayerController.kt
│   ├── VlcOptions.kt                  LibVLC option builder (incl. subtitle style)
│   └── VlcPlayerControllerFactory.kt
├── domain/
│   ├── model/                         Video, SubtitleFile, PlaybackProgress, SubtitleStyle, VideoPreferences, AppSettings
│   ├── repository/                    StorageRepository, PlaybackProgressRepository, SubtitleRepository,
│   │                                  VideoPreferencesRepository, SettingsRepository
│   └── usecase/                       ScanLibraryUseCase, ObserveLibraryUseCase, SubtitleMatcher, ...
├── data/
│   ├── local/                         (target) SubtleDatabase, DAOs, entities, mappers
│   ├── storage/                       AndroidStorageAccess, FileVideoScanner, VideoThumbnailLoader, CharsetNormalizer, SubtitleCache
│   ├── preferences/                   (target) DataStore-backed settings
│   └── repository/                    FileStorageRepository, PreferencesPlaybackProgressRepository,
│                                      CachedSubtitleRepository, PreferencesVideoPreferencesRepository, PreferencesSettingsRepository
├── di/                                DatabaseModule, DispatcherModule, PlayerModule, RepositoryModule
└── feature/
    ├── onboarding/                    OnboardingRoute, OnboardingScreen, OnboardingViewModel (first-run storage access)
    ├── main/                          MainRoute — bottom nav host (Home / Browse)
    ├── common/                        Browser shared UI: VideoRow/Cell, FolderRow/Cell, BrowserViewMode, formatSize
    ├── home/                          HomeRoute, HomeScreen, HomeViewModel, HomeUiState (all videos / series groups)
    ├── browse/                        BrowseRoute, BrowseScreen, BrowseViewModel, BrowseUiState (folder tree)
    ├── player/                        PlayerRoute, PlayerScreen, PlayerViewModel, PlayerUiState,
    │                                  controls/, gestures/, subtitle/ (sheet)
    ├── settings/                      SettingsRoute, SettingsScreen, SettingsViewModel (theme, dynamic color, decoding)
    └── about/                         AboutRoute, AboutScreen (open-source licenses, libVLC LGPL notice)
```

Resources: `app/src/main/res/values/strings.xml`, `values-in/strings.xml`, `xml/` for backup rules.

## 3. The PlayerController contract (`core/player`)

All playback goes through this interface. The UI and ViewModels never see libVLC types.

```kotlin
interface PlayerController {

    val state: StateFlow<PlayerState>

    /** Opens a source. [startPositionMs] is applied once the media is seekable. */
    suspend fun load(source: MediaSource, startPositionMs: Long = 0L)

    fun play()
    fun pause()
    fun seekTo(positionMs: Long)
    fun setSpeed(rate: Float)

    /** Playback volume as a percentage, 0..100. */
    fun setVolume(level: Int)

    fun setAspectRatio(ratio: AspectRatio)

    fun selectAudioTrack(id: Int)
    /** Pass [Track.DISABLED_ID] to turn subtitles off. */
    fun selectSubtitleTrack(id: Int)
    suspend fun addExternalSubtitle(source: SubtitleSource, select: Boolean = true)

    fun setSubtitleDelay(delayMs: Long)
    fun setAudioDelay(delayMs: Long)

    /** Binds video output into [container]. Call from the UI thread; pair with [detachSurface]. */
    fun attachSurface(container: android.view.ViewGroup)
    fun detachSurface()

    /** Releases native resources. The instance must not be used afterwards. */
    fun release()
}

data class PlayerConfig(
    val forceSoftwareDecoding: Boolean = false,
    val subtitleStyle: SubtitleStyle = SubtitleStyle(),
)

/** Fit = letterbox; Fill = stretch to the screen; Crop = zoom to fill, hiding the overflow. */
enum class AspectRatio { Fit, Fill, Crop, Ratio16x9, Ratio4x3 }

interface PlayerControllerFactory {
    fun create(config: PlayerConfig): PlayerController
}
```

```kotlin
data class PlayerState(
    val status: Status = Status.Idle,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val speed: Float = 1f,
    val volume: Int = 100,
    val aspectRatio: AspectRatio = AspectRatio.Fit,
    val isSeekable: Boolean = false,
    val audioTracks: List<Track> = emptyList(),
    val subtitleTracks: List<Track> = emptyList(),
    val selectedAudioTrackId: Int? = null,
    val selectedSubtitleTrackId: Int? = null,
    val subtitleDelayMs: Long = 0L,
    val audioDelayMs: Long = 0L,
    val error: PlayerError? = null,
) {
    enum class Status { Idle, Opening, Buffering, Playing, Paused, Ended, Error }
}

data class Track(val id: Int, val name: String) {
    companion object { const val DISABLED_ID = -1 }
}

sealed interface MediaSource { data class Content(val uri: android.net.Uri) : MediaSource }
sealed interface SubtitleSource { data class Content(val uri: android.net.Uri, val displayName: String) : SubtitleSource }

sealed interface PlayerError {
    data object UnsupportedFormat : PlayerError
    data object FileUnavailable : PlayerError
    data class Engine(val message: String?) : PlayerError
}
```

Why this shape
- One `StateFlow<PlayerState>` is the single source of truth. Engine callbacks (non-main thread) only call `state.update { }`.
- Commands are fire-and-forget; their effect is observed through `state`.
- Swapping engine (libmpv, Media3) or adding watch-together sync means implementing or wrapping this interface; the UI is untouched.

## 4. Data and persistence

Current implementation (Phase 2): no Room yet. Home and Browse read the shared storage live through `FileVideoScanner` (`java.io.File`), and `AndroidStorageAccess` gates on `MANAGE_EXTERNAL_STORAGE` (API 30+) / `READ_EXTERNAL_STORAGE` (API ≤ 29). The only persisted state is playback progress, kept in `SharedPreferences` (`PreferencesPlaybackProgressRepository`, keyed by URI, encoded `positionMs,durationMs,lastPlayedMs`, no DataStore dependency); the list/grid view mode and Home sort live in each ViewModel's `SavedStateHandle`. Both are intentionally disposable and will be replaced by the Room tables below at the scan/indexing phase.

Target schema (once scanning is indexed): Room 3 database `subtle.db`, schema export enabled, migrations tested.

| Entity | Key fields |
|---|---|
| `LibraryFolderEntity` | `treeUri` (PK), `addedAt` |
| `VideoEntity` | `id` (PK, autogenerate), `documentUri` (unique), `folderTreeUri`, `displayName`, `sizeBytes`, `lastModified`, `durationMs?`, `addedAt` |
| `SubtitleFileEntity` | `id`, `videoId` (FK, cascade), `documentUri`, `displayName`, `languageTag?` |
| `PlaybackProgressEntity` | `videoId` (PK, FK), `positionMs`, `durationMs`, `updatedAt`, `watched` |
| `VideoPreferenceEntity` | `videoId` (PK, FK), `subtitleDelayMs`, `audioDelayMs`, `selectedSubtitleKey?`, `selectedAudioKey?`, `hasHardSubs`, `charsetOverride?` |

Settings (implemented): `SharedPreferences` behind `SettingsRepository` — theme (system/light/dark), dynamic color, force software decoding, subtitle style (size, color, outline, background), default subtitle language. Read at startup by `MainActivity` (theme) and by `PlayerViewModel` before creating the engine (`PlayerConfig.forceSoftwareDecoding` / `PlayerConfig.subtitleStyle`) and when applying the subtitle-language preference.

Subtitle styling (baseline): libVLC exposes no runtime style API, so `SubtitleStyle` is mapped to `freetype-*` module options in `VlcOptions.buildVlcArgs` and applied when the engine (`LibVLC`) is created. A style change therefore takes effect on the next playback session; only text subtitles are affected. The Settings screen shows a Compose preview approximating the result.

Thumbnails: generated lazily with `MediaMetadataRetriever` on the IO dispatcher, cached to `cacheDir/thumbs/` and loaded by the image loader. They are disposable; never store them in the database.

## 5. Key flows

Startup
1. `RootViewModel` reads `StorageAccess.isGranted()`: not granted → `Onboarding`, granted → `Main` (the bottom-nav host). It re-checks on every `ON_RESUME`, so returning from the Settings permission screen flips the gate automatically.
2. Onboarding explains why full-storage access is needed and opens the "All files access" Settings screen (API 30+) or requests `READ_EXTERNAL_STORAGE` (API ≤ 29). There is no folder picker and no saved-folder flag.
3. If a video is opened via `ACTION_VIEW`, the player takes precedence over the gate.

Home
`HomeViewModel` scans the whole storage through `StorageRepository.allVideos(onProgress)`, which walks the tree on the IO dispatcher and reports `ScanProgress` (folders scanned, videos found) as it goes; the scan is cancellable through its coroutine. The UI shows a progress banner with a Cancel action and a pull-to-refresh gesture. Every completed scan replaces the list, so removed files disappear. A search field filters by file name and a toolbar menu sorts by name, date added (`Video.lastModified`) or last played (`PlaybackProgress.lastPlayedMs`); the pure `filterAndSortVideos` use case does the work, and the chosen sort lives in `SavedStateHandle`. The same menu can toggle series grouping: the pure `SeriesGrouping` use case folds `S01E02`/`1x02`-style names into `SeriesGroup`s that the list renders as expandable headers. Rescan-on-start is a fresh non-blocking scan (no persistent index yet).

Browse
`BrowseViewModel` keeps a breadcrumb stack and calls `StorageRepository.listDirectory(path)` per directory (null = storage root). Only subfolders that contain videos (recursively) are listed, alongside the videos in the current directory. System `Android/` and hidden folders are skipped.

Scan (target)
1. `ScanLibraryUseCase` → scanner walks the tree, emitting `ScanEvent`s (directory by directory, collecting videos and subtitle files of the same directory).
2. `SubtitleMatcher` pairs subtitles to videos per directory.
3. Repository upserts in batches and deletes rows for files that no longer exist. UI observes `Flow<List<Video>>` from Room.

Play
1. `PlayerRoute` creates `PlayerViewModel` around the opened URI. The ViewModel creates a `PlayerController` from the factory and resolves a same-folder playlist via `StorageRepository.listDirectory(parent)` so prev/next walk the siblings.
2. On open the ViewModel reads the saved `PlaybackProgress` and calls `load(source, resumePositionMs)`. `resumePositionMs()` restarts from 0 when the record is under 5 s in, within 5 s of the end, or has no duration — so finished/opening credits do not resume.
3. Position is auto-saved every 10 s while playing, and on pause and on leaving the player (`stop()`/`onCleared`). Reaching the end clears the record instead of storing 100%. The `PlayerScreen` "restart from beginning" action seeks to 0 and plays.
4. The composable binds video output to the lifecycle: it calls `attachSurface` on `ON_START` (and immediately if the effect appears while already foregrounded) and `detachSurface` on `ON_STOP`, so backgrounding detaches the video output while audio keeps playing and returning re-attaches a fresh surface. `onDispose` also detaches. The ViewModel calls `release()` in `onCleared`.
5. Back navigation: player → close; Browse inside a folder → up one level; Browse at root → Home tab; Home → exit.

Progress display
Home and Browse observe `PlaybackProgressRepository.progress` (a `Flow<Map<uri, PlaybackProgress>>`) and pass each item's `fraction` to `VideoRow`/`VideoCell`, which draw a thin `LinearProgressIndicator` over the thumbnail.

Subtitle sheet
`PlayerState.subtitleTracks` (embedded) + `Video.subtitleFiles` (external, from DB) → merged list in `PlayerViewModel` → sheet. Choosing an external file calls `addExternalSubtitle`, then records the choice in `VideoPreference`.

Default subtitle language
`AppSettings.subtitleLanguage` (`None`/`English`/`Indonesian`). On open, `PlayerViewModel` first tries an external file whose `languageHint` matches (`SubtitleLanguageMatcher`, token-based), then, once external discovery has finished and no external matched, an embedded track whose name matches. `None` leaves the choice to the engine, and the hard-subtitle policy wins over both.

Background audio
`PlayerViewModel` (the owner of the engine) mirrors the title, play/pause state, and prev/next availability into the process-wide `PlaybackHolder`, which starts and stops a foreground `PlaybackService`. The service keeps the process alive while the app is backgrounded and shows a notification with play/pause and, when available, previous/next controls; its actions call back into the ViewModel through the holder. Leaving the player (`stop()`) detaches the holder and stops the service. Auto-play-next and series grouping both live in the player/Home ViewModels. On API 33+ the app requests `POST_NOTIFICATIONS`; the service runs regardless but its controls are only visible once granted.

## 6. Threading

- UI and `attachSurface`/`detachSurface`: main thread.
- libVLC commands: serialized on one dedicated single-thread dispatcher (`@PlayerDispatcher`). `stop()` and `release()` must never run on the main thread.
- IO (scan, thumbnails, charset detection, cache copy): `@IoDispatcher`.
- All callbacks from the engine arrive on engine threads; they must only update the `StateFlow`.

## 7. Error handling

- Domain and data return typed results (`sealed interface` or `Result`); exceptions are converted at the boundary. Always rethrow `CancellationException`.
- The player exposes `PlayerError`; `PlayerScreen` shows a message with "Back to library" and, for `UnsupportedFormat`, a hint about software decoding.
- Missing storage access → `RootViewModel` routes back to onboarding.

## 8. Testing

- Unit: domain (`SubtitleMatcher`, use cases), ViewModels (with `FakePlayerController`, fake repositories), mappers. Use Turbine for flows and the coroutines test dispatcher.
- Integration (instrumented): Room DAOs and migrations; `SafScanner` against a test document provider.
- UI: Compose tests for `LibraryScreen` and the subtitle sheet with fake state.
- Manual test corpus (not in repo, never commit media): MP4/H.264, MKV/HEVC, multi-audio, embedded SRT/ASS/PGS, external `.srt` in CP1252 and UTF-8, hardsubbed file, very large file, corrupt file.

## 9. Build baseline

`settings.gradle.kts`
```kotlin
pluginManagement {
    repositories { google(); mavenCentral(); gradlePluginPortal() }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories { google(); mavenCentral() }
}
rootProject.name = "Subtle"
include(":app")
```

Root `build.gradle.kts`
```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
}
```

`app/build.gradle.kts`
```kotlin
plugins {
    alias(libs.plugins.android.application)   // AGP 9 built-in Kotlin: do NOT apply org.jetbrains.kotlin.android
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "app.subtle.player"
    compileSdk = 37

    defaultConfig {
        applicationId = "app.subtle.player"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures { compose = true }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)

    implementation(libs.androidx.room3.runtime)
    ksp(libs.androidx.room3.compiler)
    implementation(libs.androidx.sqlite.bundled)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)

    implementation(libs.libvlc.all)
}
```

Libraries not yet in the catalog and needed later (look up the latest stable first): DataStore, an image loader for thumbnails (Coil), `androidx.documentfile` (only if needed), kotlinx-coroutines-test, Turbine, JUnit, Compose UI test.

Gradle wrapper: 9.5.0 or newer (AGP 9.3 minimum). Enable the configuration cache and build cache in `gradle.properties`.

### 9a. Phase 1 pins (2026-10-16, verified against Google Maven / Maven Central / Gradle Plugin Portal)

- AGP 9.4.1 (kept: newer than the 9.3.x baseline, latest stable at the time).
- Kotlin 2.3.21, KSP 2.3.12 (new-scheme versioning, first line supporting AGP 9 built-in Kotlin).
  Kotlin 2.4.x is deferred: KSP only supports KGP up to 2.3.x, and Hilt + Room 3 both require KSP.
- Hilt 2.60.1, Room 3.0.3, Compose BOM 2026.09.00, Navigation 3 1.2.0, libVLC 3.7.7,
  lifecycle 2.11.0, activity 1.13.0, coroutines 1.11.0, Robolectric 4.17, serialization 1.11.0.
- Room 3, Navigation 3, DataStore and serialization are versioned in the catalog but not
  wired into `:app` until the phase that needs them (2, 3, 5). No alpha/beta anywhere.
- `PlayerControllerFactory` is a `fun interface` (SAM) so tests can fake it with a lambda.
- Unit tests needing `android.net.Uri` run under Robolectric; the test JVM carries the
  `--add-opens`/`--enable-native-access` flags JDK 21 requires (see `app/build.gradle.kts`).

Release hardening (implemented in `app/build.gradle.kts`): R8 minify + resource shrinking on for `release`, keep rules for libVLC (`org.videolan.libvlc.**`, JNI-accessed classes) in `proguard-rules.pro`; verify with a release build on a device, since missing keep rules fail only at runtime. Per-ABI APKs via `splits { abi { ... } }` (arm64-v8a, armeabi-v7a, x86_64; each < 70 MB) and an AAB via `:app:bundleRelease`. Signing is opt-in through a git-ignored `keystore.properties`; without it the build produces unsigned APKs. Manual QA steps live in `docs/RELEASE.md`.

## 10. Manifest notes

- Storage: `MANAGE_EXTERNAL_STORAGE` (All files access, API 30+) and `READ_EXTERNAL_STORAGE` with `maxSdkVersion="29"`; `android:requestLegacyExternalStorage="true"` for API 29. No `INTERNET` permission in v1.
- `MainActivity`: `android:supportsPictureInPicture="true"`, `android:configChanges="orientation|screenSize|smallestScreenSize|screenLayout|keyboardHidden"` so rotation and PiP resizing do not recreate the player.
- Intent filter for `ACTION_VIEW` with `video/*` (and `content` scheme) to support "Open with".
- Background audio: `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_MEDIA_PLAYBACK` and a non-exported `PlaybackService` with `foregroundServiceType="mediaPlayback"`; `POST_NOTIFICATIONS` is requested at startup on API 33+.
- `android:allowBackup` with explicit `dataExtractionRules`/`fullBackupContent` that exclude cache and thumbnails.

## 11. Evolution path

- Watch-together (later): add a `SyncSession` in `domain` that sends/receives play, pause, seek events and wraps a `PlayerController`. Transport (WebSocket) lives in `data`; the engine contract does not change.
- Other engines: implement `PlayerController` (e.g. libmpv for best ASS rendering) and swap in `di/PlayerModule`.
- KMP: only `domain` is portable today by design; move it to a shared module if iOS or desktop ever becomes a goal.
