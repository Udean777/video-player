# Release checklist — Subtle

Manual steps that a unit test cannot cover. Run on real hardware (at least two vendors).

## Build

```bash
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
./gradlew :app:assembleRelease        # signed per-ABI APKs, or -unsigned without a keystore
./gradlew :app:bundleRelease          # AAB for Play
```

Release APKs are split per ABI via `splits.abi` (arm64-v8a, armeabi-v7a, x86_64); each stays under
the 70 MB budget. Signing is enabled by dropping a `keystore.properties` (git-ignored) next to
`settings.gradle.kts`:

```properties
storeFile=/absolute/path/to/release.jks
storePassword=…
keyAlias=…
keyPassword=…
```

Without that file the release build produces unsigned APKs and R8 minification + resource
shrinking still run (keep rules in `app/proguard-rules.pro`).

## Test corpus (≥ 60 files)

Cover containers MP4, MKV, AVI, MOV, WebM; video H.264, HEVC, VP9, AV1; audio AAC, AC3/EAC3, DTS,
Opus; subtitles embedded (SRT, ASS, PGS, VobSub) and external (`.srt`/`.ass`/`.vtt`, with and
without a language suffix, some non-UTF-8). Target: 95% play without manual intervention.

## Device checks

- Cold start to first frame < 2 s (H.264 1080p, mid-range device).
- 2-hour soak with seeking and track switching: zero crashes.
- Hardware decode per chipset; the "force software decoding" toggle fixes bad playback.
- HEVC, DTS/EAC3 and PGS subtitles on any engine or version change.
- Subtitle style (size note: VLC `freetype-rel-fontsize` — a **smaller** value is larger text;
  color, outline, background) applies from the next playback; ASS keeps file styling.
- Gestures: brightness (left), volume (right), horizontal seek, double-tap ±10 s, lock (also locks
  orientation), unlock while locked.
- Aspect-ratio modes (fit, fill, crop, 16:9, 4:3) zoom correctly per codec.
- Search, sort (name / date added / last played), default subtitle language auto-selection.
- Series grouping: grouped episodes sort inside an expandable header; unmarked files stay as singles.
- Auto-play next moves to the sibling video at the end; disabled, playback just stops.
- Background audio: home button keeps audio playing with a notification; play/pause and prev/next
  controls work; leaving the player stops the service. On API 33+ the notification permission prompt.
- In-app language switch (system/English/Indonesian) applies immediately on API 33+.
- Scan: progress banner + cancel; removed files disappear after refresh; pull-to-refresh.
- Picture-in-picture, "Open with" from a file manager, process-death restore, rotation.
- Permission flow: `MANAGE_EXTERNAL_STORAGE` (API 30+) / `READ_EXTERNAL_STORAGE` (≤ 29).

## Accessibility

Screen-reader labels on all controls, 48 dp touch targets, font scaling to 200%, contrast in both
themes. Verify with TalkBack and the display-size/font-size settings at maximum.
