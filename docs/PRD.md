# PRD — Subtle (working name)

Status: Draft v0.1 · Platform: Android · Last updated: 2026-10-16

## 1. Summary

Subtle is a video player for Android that plays any local video file and makes subtitles easy:
embedded tracks, external files, charset problems, timing offsets, and videos that already have
burned-in (hard) subtitles. Reference point for format support and feel: VLC for Android.

A watch-together (remote sync) mode is a later phase. The architecture keeps room for it,
but nothing in v1 depends on a network.

## 2. Problem

Local video collections come in many shapes: MKV with several audio/subtitle tracks, `.srt` files
next to the video, subtitles in legacy encodings, subtitles that drift, and releases with
hardsubs where a second subtitle overlaps the first. Stock players handle some of this
poorly. VLC handles most of it but its subtitle controls are buried and its UI is dated.

## 3. Goals and non-goals

Goals
- G1. Play common containers and codecs reliably (MP4, MKV, AVI, MOV, WebM; H.264, HEVC, VP9, AV1; AAC, AC3/EAC3, DTS, Opus).
- G2. Make subtitle selection, loading, timing and styling fast to reach and hard to get wrong.
- G3. Grant storage access once, then browse folders and resume from a clean library.
- G4. Stay private: everything local, no account, no telemetry.
- G5. Stay responsive: cold start to first frame in under 2 s for a local 1080p H.264 file on a mid-range device.

Non-goals for v1
- Network streaming, DLNA, cast, torrent, DRM content
- Downloading or searching online subtitles (planned P2, needs API key and legal review)
- Transcoding or editing
- Watch-together / remote sync (Phase 3)
- iOS, desktop, TV (Android handset and tablet only)

## 4. Users

Primary: a person with a personal library of downloaded movies and series on a phone or tablet,
who regularly deals with external subtitles and multi-track MKVs.
Secondary: a couple watching together later (Phase 3).

## 5. Functional requirements

Priority: P0 = required for v1, P1 = v1.1, P2 = later.

### 5.1 Library and storage

| ID | Requirement | Pri |
|---|---|---|
| L1 | First launch asks for full-storage access ("All files access" on API 30+, `READ_EXTERNAL_STORAGE` below); no folder picker | P0 |
| L2 | Recursive scan runs in the background, shows progress, can be cancelled, never blocks the UI | P0 |
| L3 | Library lists videos with thumbnail, title (from file name), duration, and watched progress | P0 |
| L5 | Re-scan on app start (incremental) and on pull-to-refresh; removed files disappear | P0 |
| L6 | Search by title; sort by name, date added, last played | P1 |
| L7 | "Open with" from a file manager plays a single file without adding it to the library | P0 |
| L8 | Series grouping by season/episode pattern | P2 |
| L9 | Two bottom-nav tabs: Home (every video) and Browse (internal-storage folder tree, VLC-style) | P0 |

### 5.2 Playback

| ID | Requirement | Pri |
|---|---|---|
| P1 | Play, pause, seek (slider, double-tap ±10 s, swipe), playback speed 0.25x–4x, restart from beginning | P0 |
| P2 | Resume from last position per video, shown as a progress bar in the library; reaching the end clears the position (no lingering 100%) | P0 |
| P3 | Audio track selection, audio delay adjustment | P0 |
| P4 | Hardware decoding by default with automatic fallback; settings toggle to force software | P0 |
| P5 | Gestures: brightness (left), volume (right), seek (horizontal); lock controls | P1 |
| P6 | Picture-in-picture; keep screen on while playing; correct behavior on rotation and process death | P0 |
| P7 | Aspect ratio modes: fit, fill, crop, 16:9, 4:3 | P1 |
| P8 | Background audio playback with notification controls | P2 |
| P9 | Next/previous episode, auto-play next | P2 |

### 5.3 Subtitles (core feature)

| ID | Requirement | Pri |
|---|---|---|
| S1 | List embedded subtitle tracks with readable names (language, title, codec); select or turn off | P0 |
| S2 | Support text formats: SRT, ASS/SSA, VTT, mov_text; image formats: PGS, VobSub | P0 |
| S3 | Auto-detect external subtitle files in the same folder as the video (same base name, optionally with a language suffix like `.en.srt`) and offer them in the track list | P0 |
| S4 | Load a subtitle manually via the system file picker; remember it for that video | P0 |
| S5 | Subtitle delay adjustment with ±100 ms steps and a fine ±10 ms mode; reset button; value remembered per video | P0 |
| S6 | Charset handling: detect non-UTF-8 text subtitles automatically; let the user override the charset when text looks broken | P0 |
| S7 | Style (text subtitles): text size, color, outline/shadow, background opacity. Applied when a video is opened | P1 |
| S8 | Hard-subtitle mode: user marks a video as "has burned-in subtitles"; soft subtitles are hidden for it (default behavior), remembered per video | P0 |
| S10 | Default subtitle language preference; auto-select matching track on playback start | P1 |

### 5.4 Settings and misc

| ID | Requirement | Pri |
|---|---|---|
| M1 | Theme: system / light / dark; Material You dynamic color when available | P0 |
| M2 | Languages: English and Indonesian | P1 |
| M3 | About screen with open-source licenses and the libVLC LGPL notice | P0 |

## 6. Non-functional requirements

- Performance: scrolling a 1,000-item library stays at 60 fps on a mid-range device; library scan of 1,000 files completes in under 30 s on local storage.
- Battery: no wake locks outside active playback; no background work after the app is closed.
- Size: release APK per ABI under 70 MB (libVLC dominates). Ship as an AAB.
- Privacy: no network permission in v1. No analytics, crash reporters, or ads. Do not log file names in release builds.
- Accessibility: screen-reader labels on all controls, 48 dp touch targets, supports font scaling up to 200%, sufficient contrast in both themes.
- Compatibility: Android 8.0 (API 26) and above; phones and tablets; portrait and landscape.
- Reliability: a corrupt or unsupported file shows a clear error and returns to the library, never crashes.

## 7. Key flows

First run: Welcome (explain storage access) → grant "All files access" → Home.
Play: Home or Browse → tap video → player starts at last position (or from the start if nearly finished) → controls auto-hide.
Back: in the player closes it → in Browse inside a folder goes up one level → in Browse at the root switches to the Home tab → in Home exits the app.
Subtitles in player: tap subtitle button → bottom sheet with: track list (embedded + detected), "Load file…", delay stepper, style, "This video has burned-in subtitles" switch.

## 8. Screens

1. Onboarding (storage access)
2. Home (all videos, grid or list) + Browse (folder tree) behind a bottom nav
3. Player (video, controls, gesture layer, subtitle and audio sheets)
4. Settings (playback, subtitles default style, theme, language)
5. About / licenses

## 9. Success metrics (local, observable by the developer only)

- Plays 95% of a test corpus (≥ 60 files across container/codec/subtitle combinations) without manual intervention
- Zero crashes in a 2-hour soak test with seeking and track switching
- Cold start to first frame < 2 s (H.264 1080p, mid-range device)

## 10. Phases

| Phase | Scope |
|---|---|
| 1 | Project setup, engine integration, play one file from a `content://` URI, `PlayerController` |
| 2 | Storage access, Home + Browse tabs, live filesystem scan, resume |
| 3 | Core player controls, lifecycle, PiP |
| 4 | Subtitles: embedded tracks, external files, delay, charset, hardsub mode |
| 5 | Gestures, audio tracks, styling, settings, localization |
| 6 | Polish, accessibility pass, test corpus, release build |
| Later | Watch-together sync |

## 11. Risks

| Risk | Mitigation |
|---|---|
| libVLC 4.x is still early-access; 3.7.x is the stable line | Use 3.7.x behind `PlayerController`; revisit when 4.x is stable |
| libVLC may not read `content://` URIs for external subtitles on all devices | Fall back to a cached `file://` copy (see `saf-storage` skill) |
| Hardware decoding bugs on specific chipsets | Software fallback toggle; test on at least two devices from different vendors |
| LGPL obligations | Dynamic linking, license screen, source link |
| APK size from native libraries | AAB with ABI splits; arm64-v8a first |
| Image subtitles (PGS) cannot be restyled | Document the limitation in the UI; only offer styling for text subtitles |

## 12. Open questions

- Final app name and package id (currently `app.subtle.player`, placeholder)
- Distribute on Google Play, F-Droid, or both (affects licensing and tracking-free claims)
- Is background audio worth moving up to v1? — Yes, implemented: a foreground playback service with play/pause and next/previous notification controls.

## 13. Removed (not feasible on libVLC 3.7 / out of scope)

Dropped from the requirements above; kept here so the reasoning is not lost.

| Was | Why removed |
|---|---|
| Subtitle bottom offset / vertical position (part of S7) | libVLC 3.7 exposes no runtime or option-based control for subtitle vertical position |
| Live subtitle styling without reopening the video (part of S7) | Style is a freetype module option set at engine creation; changing it needs a new `LibVLC` instance, so it applies from the next playback instead |
| Hard-sub avoidance by repositioning soft subtitles (S9) | Same as position: requires runtime repositioning the engine does not provide |
| Online subtitle search (S11) | v1 is offline with no network permission (see Non-goals) |
| Multiple library folders add/remove (L4) | Superseded by the all-files scan model; the library is every video reachable through storage access |
