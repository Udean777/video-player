package app.subtle.player.feature.player

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.widget.FrameLayout
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import app.subtle.player.R
import app.subtle.player.core.designsystem.SubtleTheme
import app.subtle.player.core.player.PlayerController
import app.subtle.player.core.player.PlayerError
import app.subtle.player.core.player.PlayerState
import kotlinx.coroutines.delay

@Composable
fun PlayerScreen(
    state: PlayerUiState,
    controller: PlayerController?,
    sliderPositionMs: Long,
    onOpenFile: () -> Unit,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onRestart: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSkipBackward: () -> Unit,
    onSkipForward: () -> Unit,
    onSliderChange: (Long) -> Unit,
    onSliderChangeFinished: () -> Unit,
    modifier: Modifier = Modifier,
    onClose: (() -> Unit)? = null,
) {
    val playing = state.player.status == PlayerState.Status.Playing
    var controlsVisible by remember { mutableStateOf(true) }
    var orientationLocked by rememberSaveable { mutableStateOf(false) }

    // Bars show with the controls and hide for immersive playback.
    PlayerSystemBars(controlsVisible)
    // Follow the device by default; locking freezes the current orientation.
    PlayerOrientation(orientationLocked)
    KeepScreenOn(playing)

    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        val container = remember { mutableStateOf<FrameLayout?>(null) }
        AndroidView(
            factory = { context -> FrameLayout(context).also { container.value = it } },
            modifier = Modifier.fillMaxSize(),
        )

        val attached = container.value
        if (attached != null) {
            DisposableEffect(attached, controller) {
                controller?.attachSurface(attached)
                onDispose { controller?.detachSurface() }
            }
        }

        // Tap anywhere to toggle the controls; only interactive overlay children consume taps.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) { detectTapGestures(onTap = { controlsVisible = !controlsVisible }) },
        )

        if (state.videoUri == null) {
            Text(
                text = stringResource(R.string.no_video_chosen),
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.Center).padding(24.dp),
            )
        }

        LaunchedEffect(controlsVisible, state.player.status) {
            if (controlsVisible && playing) {
                delay(CONTROLS_TIMEOUT_MS)
                controlsVisible = false
            }
        }

        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize(),
        ) {
            PlayerControls(
                state = state,
                sliderPositionMs = sliderPositionMs,
                orientationLocked = orientationLocked,
                onToggleOrientationLock = { orientationLocked = !orientationLocked },
                onClose = onClose,
                onOpenFile = onOpenFile,
                onPlay = onPlay,
                onPause = onPause,
                onRestart = onRestart,
                onPrevious = onPrevious,
                onNext = onNext,
                onSkipBackward = onSkipBackward,
                onSkipForward = onSkipForward,
                onSliderChange = onSliderChange,
                onSliderChangeFinished = onSliderChangeFinished,
            )
        }
    }
}

@Composable
private fun BoxScope.PlayerControls(
    state: PlayerUiState,
    sliderPositionMs: Long,
    orientationLocked: Boolean,
    onToggleOrientationLock: () -> Unit,
    onClose: (() -> Unit)?,
    onOpenFile: () -> Unit,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onRestart: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSkipBackward: () -> Unit,
    onSkipForward: () -> Unit,
    onSliderChange: (Long) -> Unit,
    onSliderChangeFinished: () -> Unit,
) {
    val playing = state.player.status == PlayerState.Status.Playing
    val buffering = state.player.status == PlayerState.Status.Opening ||
        state.player.status == PlayerState.Status.Buffering
    val error = state.error

    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.35f))) {
        Row(
            modifier = Modifier.fillMaxWidth().align(Alignment.TopStart).statusBarsPadding()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onClose != null) {
                IconButton(onClick = onClose) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back), tint = Color.White)
                }
            }
            Text(
                text = state.videoUri?.lastPathSegment ?: stringResource(R.string.app_name),
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onRestart) {
                Icon(Icons.Filled.RestartAlt, stringResource(R.string.restart_video), tint = Color.White)
            }
            IconButton(onClick = onToggleOrientationLock) {
                Icon(
                    imageVector = if (orientationLocked) Icons.Filled.Lock else Icons.Filled.LockOpen,
                    contentDescription = stringResource(
                        if (orientationLocked) R.string.unlock_orientation else R.string.lock_orientation,
                    ),
                    tint = Color.White,
                )
            }
            IconButton(onClick = onOpenFile) {
                Icon(Icons.Filled.FolderOpen, stringResource(R.string.open_video), tint = Color.White)
            }
        }

        // The transport row is deliberately unpadded so it stays at the true screen center.
        if (error != null) {
            Text(
                text = errorMessage(error),
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.Center).padding(24.dp),
            )
        } else {
            Row(
                modifier = Modifier.align(Alignment.Center),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                TransportButton(Icons.Filled.SkipPrevious, R.string.previous_video, state.hasPrevious, onPrevious)
                TransportButton(Icons.Filled.Replay10, R.string.rewind_10, true, onSkipBackward)
                if (buffering) {
                    Box(modifier = Modifier.size(72.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color.White, strokeWidth = 3.dp, modifier = Modifier.size(40.dp))
                    }
                } else {
                    IconButton(onClick = { if (playing) onPause() else onPlay() }, modifier = Modifier.size(72.dp)) {
                        Icon(
                            imageVector = if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = stringResource(if (playing) R.string.pause else R.string.play),
                            tint = Color.White,
                            modifier = Modifier.size(56.dp),
                        )
                    }
                }
                TransportButton(Icons.Filled.Forward10, R.string.forward_10, true, onSkipForward)
                TransportButton(Icons.Filled.SkipNext, R.string.next_video, state.hasNext, onNext)
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter).navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(formatTime(sliderPositionMs), color = Color.White, style = MaterialTheme.typography.labelMedium)
                Text(formatTime(state.player.durationMs), color = Color.White, style = MaterialTheme.typography.labelMedium)
            }
            Slider(
                value = sliderPositionMs.toFloat(),
                onValueChange = { onSliderChange(it.toLong()) },
                onValueChangeFinished = onSliderChangeFinished,
                valueRange = 0f..state.player.durationMs.coerceAtLeast(1L).toFloat(),
                enabled = state.player.isSeekable && state.videoUri != null,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun TransportButton(icon: ImageVector, @StringRes contentDescriptionRes: Int, enabled: Boolean, onClick: () -> Unit) {
    IconButton(onClick = onClick, enabled = enabled, modifier = Modifier.size(48.dp)) {
        Icon(
            imageVector = icon,
            contentDescription = stringResource(contentDescriptionRes),
            tint = Color.White.copy(alpha = if (enabled) 1f else 0.3f),
            modifier = Modifier.size(28.dp),
        )
    }
}

@Composable
private fun PlayerSystemBars(visible: Boolean) {
    val view = LocalView.current
    if (view.isInEditMode) return
    val activity = view.context.findActivity()

    LaunchedEffect(activity, visible) {
        val controller = activity?.let { WindowCompat.getInsetsController(it.window, view) } ?: return@LaunchedEffect
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        if (visible) {
            controller.show(WindowInsetsCompat.Type.systemBars())
        } else {
            controller.hide(WindowInsetsCompat.Type.systemBars())
        }
    }

    DisposableEffect(activity) {
        onDispose {
            activity?.let { WindowCompat.getInsetsController(it.window, view).show(WindowInsetsCompat.Type.systemBars()) }
        }
    }
}

@Composable
private fun PlayerOrientation(locked: Boolean) {
    val view = LocalView.current
    if (view.isInEditMode) return
    val activity = view.context.findActivity() ?: return
    val original = remember(activity) { activity.requestedOrientation }

    LaunchedEffect(activity, locked) {
        activity.requestedOrientation =
            if (locked) ActivityInfo.SCREEN_ORIENTATION_LOCKED else ActivityInfo.SCREEN_ORIENTATION_SENSOR
    }

    DisposableEffect(activity) {
        onDispose { activity.requestedOrientation = original }
    }
}

@Composable
private fun KeepScreenOn(enabled: Boolean) {
    val view = LocalView.current
    DisposableEffect(enabled) {
        view.keepScreenOn = enabled
        onDispose { view.keepScreenOn = false }
    }
}

@Composable
private fun errorMessage(error: PlayerError): String =
    when (error) {
        PlayerError.UnsupportedFormat -> stringResource(R.string.player_error_unsupported)
        PlayerError.FileUnavailable -> stringResource(R.string.player_error_unavailable)
        is PlayerError.Engine -> stringResource(R.string.player_error_engine)
    }

private fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}

private fun formatTime(positionMs: Long): String {
    val totalSeconds = (positionMs.coerceAtLeast(0L) / 1000).toInt()
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
    }
}

private const val CONTROLS_TIMEOUT_MS = 3_000L

@Preview(showBackground = true)
@Composable
private fun PlayerScreenEmptyPreview() {
    SubtleTheme(darkTheme = false) {
        PreviewPlayer(state = PlayerUiState())
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PlayerScreenPlayingPreview() {
    SubtleTheme(darkTheme = true) {
        PreviewPlayer(
            state = PlayerUiState(
                player = PlayerState(
                    status = PlayerState.Status.Playing,
                    positionMs = 65_000L,
                    durationMs = 600_000L,
                    isSeekable = true,
                ),
                hasPrevious = true,
                hasNext = true,
            ),
        )
    }
}

@Composable
private fun PreviewPlayer(state: PlayerUiState) {
    PlayerScreen(
        state = state,
        controller = null,
        sliderPositionMs = state.player.positionMs,
        onOpenFile = {},
        onPlay = {},
        onPause = {},
        onRestart = {},
        onPrevious = {},
        onNext = {},
        onSkipBackward = {},
        onSkipForward = {},
        onSliderChange = {},
        onSliderChangeFinished = {},
    )
}
