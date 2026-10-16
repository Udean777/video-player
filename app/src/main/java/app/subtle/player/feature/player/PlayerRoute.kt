package app.subtle.player.feature.player

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

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

    // Releasing the engine here (not only in onCleared) stops audio as soon as the screen is left.
    DisposableEffect(Unit) {
        onDispose { viewModel.stop() }
    }

    PlayerScreen(
        state = uiState,
        controller = controller,
        sliderPositionMs = if (dragging) dragPositionMs else uiState.player.positionMs,
        onOpenFile = { picker.launch(arrayOf("video/*")) },
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
}
