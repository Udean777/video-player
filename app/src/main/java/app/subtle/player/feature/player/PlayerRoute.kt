package app.subtle.player.feature.player

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.subtle.player.domain.model.SubtitleFile
import app.subtle.player.feature.player.playback.PlaybackSheet
import app.subtle.player.feature.player.subtitle.SubtitleSheet

@Composable
fun PlayerRoute(
    viewModel: PlayerViewModel = viewModel(),
    initialUri: Uri? = null,
    onClose: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val controller by viewModel.controllerFlow.collectAsStateWithLifecycle()

    val picker =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                runCatching {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION,
                    )
                }
                viewModel.open(uri)
            }
        }

    val subtitlePicker =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                runCatching {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION,
                    )
                }
                val name = context.queryDisplayName(uri) ?: uri.lastPathSegment ?: "subtitle"
                viewModel.loadSubtitle(SubtitleFile(uri.toString(), name, null))
            }
        }

    LaunchedEffect(initialUri) {
        if (initialUri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    initialUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
            viewModel.openInitial(initialUri)
        }
    }

    var dragging by remember { mutableStateOf(false) }
    var dragPositionMs by remember { mutableLongStateOf(0L) }
    var subtitleSheetVisible by rememberSaveable { mutableStateOf(false) }
    var playbackSheetVisible by rememberSaveable { mutableStateOf(false) }

    // Releasing the engine here (not only in onCleared) stops audio as soon as the screen is left.
    DisposableEffect(Unit) {
        onDispose { viewModel.stop() }
    }

    PlayerScreen(
        state = uiState,
        controller = controller,
        sliderPositionMs = if (dragging) dragPositionMs else uiState.player.positionMs,
        onOpenFile = { picker.launch(arrayOf("video/*")) },
        onOpenSubtitles = { subtitleSheetVisible = true },
        onOpenPlayback = { playbackSheetVisible = true },
        onClose = onClose,
        onPlay = viewModel::play,
        onPause = viewModel::pause,
        onRestart = viewModel::restart,
        onPrevious = viewModel::previous,
        onNext = viewModel::next,
        onSkipBackward = viewModel::skipBackward,
        onSkipForward = viewModel::skipForward,
        onSliderChange = {
            dragging = true
            dragPositionMs = it
        },
        onSliderChangeFinished = {
            dragging = false
            viewModel.seekTo(dragPositionMs)
        },
    )

    if (subtitleSheetVisible) {
        SubtitleSheet(
            player = uiState.player,
            externalSubtitles = uiState.externalSubtitles,
            preferences = uiState.videoPreferences,
            onDismiss = { subtitleSheetVisible = false },
            onSelectTrack = viewModel::selectSubtitleTrack,
            onLoadSubtitle = viewModel::loadSubtitle,
            onPickFile = { subtitlePicker.launch(arrayOf("*/*")) },
            onDelayDelta = viewModel::adjustSubtitleDelay,
            onResetDelay = viewModel::resetSubtitleDelay,
            onHardSubsChange = viewModel::setHardSubs,
            onCharsetChange = viewModel::setCharsetOverride,
        )
    }

    if (playbackSheetVisible) {
        PlaybackSheet(
            player = uiState.player,
            onDismiss = { playbackSheetVisible = false },
            onSelectAudioTrack = viewModel::selectAudioTrack,
            onAudioDelayDelta = viewModel::adjustAudioDelay,
            onResetAudioDelay = viewModel::resetAudioDelay,
            onSpeedChange = viewModel::setSpeed,
        )
    }
}

private fun Context.queryDisplayName(uri: Uri): String? = runCatching {
    contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) cursor.getString(0) else null
    }
}.getOrNull()
