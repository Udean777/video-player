package app.subtle.player.feature.player

import android.net.Uri
import androidx.core.net.toUri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.subtle.player.core.common.PlayerDispatcher
import app.subtle.player.core.player.MediaSource
import app.subtle.player.core.player.PlayerConfig
import app.subtle.player.core.player.PlayerController
import app.subtle.player.core.player.PlayerControllerFactory
import app.subtle.player.core.player.PlayerError
import app.subtle.player.core.player.PlayerState
import app.subtle.player.domain.model.Video
import app.subtle.player.domain.repository.PlaybackProgressRepository
import app.subtle.player.domain.repository.StorageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val controllerFactory: PlayerControllerFactory,
    private val storageRepository: StorageRepository,
    private val progressRepository: PlaybackProgressRepository,
    private val savedStateHandle: SavedStateHandle,
    @PlayerDispatcher private val playerDispatcher: CoroutineDispatcher,
) : ViewModel() {

    private val _videoUri = MutableStateFlow<Uri?>(
        savedStateHandle.get<String>(KEY_VIDEO_URI)?.let(Uri::parse),
    )
    private val _playerState = MutableStateFlow(PlayerState())
    private val _fatalError = MutableStateFlow<PlayerError?>(null)
    private val _playlist = MutableStateFlow<List<Video>>(emptyList())
    private val _index = MutableStateFlow(-1)

    val uiState: StateFlow<PlayerUiState> =
        combine(_videoUri, _playerState, _fatalError, _playlist, _index) { uri, player, fatal, playlist, index ->
            PlayerUiState(
                videoUri = uri,
                player = player,
                fatalError = fatal,
                hasPrevious = index > 0,
                hasNext = index >= 0 && index < playlist.lastIndex,
            )
        }.stateIn(viewModelScope, SharingStarted.Eagerly, PlayerUiState())

    private var controller: PlayerController? = null
    private var loadJob: Job? = null
    private var lastAutoSaveMs = 0L
    private val _controllerFlow = MutableStateFlow<PlayerController?>(null)

    /** Emits the active controller so the UI can bind the video output once it exists. */
    val controllerFlow: StateFlow<PlayerController?> = _controllerFlow.asStateFlow()

    init {
        _videoUri.value?.let { open(it) }
    }

    /**
     * Opens [uri] when the player has no active playback for it. Re-entering the same video after
     * [stop] must reload, otherwise the screen would come back with a released engine.
     */
    fun openInitial(uri: Uri) {
        val alreadyActive = _videoUri.value == uri && (controller != null || loadJob?.isActive == true)
        if (!alreadyActive) {
            open(uri)
        }
    }

    fun open(uri: Uri) {
        savedStateHandle[KEY_VIDEO_URI] = uri.toString()
        _videoUri.value = uri
        _fatalError.value = null
        _playlist.value = emptyList()
        _index.value = -1
        loadJob?.cancel()
        // LibVLC construction and the first decode are heavy; keep them off the main thread.
        loadJob = viewModelScope.launch(playerDispatcher) {
            val active = controller ?: createController() ?: return@launch
            val startMs = runCatching { progressRepository.get(uri.toString())?.resumePositionMs() ?: 0L }
                .getOrDefault(0L)
            try {
                active.load(MediaSource.Content(uri), startMs)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _fatalError.value = PlayerError.Engine(e.message)
            }
            resolvePlaylist(uri)
        }
    }

    fun play() {
        controller?.play()
    }

    fun pause() {
        controller?.pause()
        saveProgress()
    }

    fun seekTo(positionMs: Long) {
        controller?.seekTo(positionMs)
    }

    /** Jump back to the start and play, whatever the current position. */
    fun restart() {
        controller?.seekTo(0L)
        controller?.play()
    }

    fun skipForward() = seekRelative(SKIP_STEP_MS)

    fun skipBackward() = seekRelative(-SKIP_STEP_MS)

    fun next() {
        _playlist.value.getOrNull(_index.value + 1)?.let { open(it.uri.toUri()) }
    }

    fun previous() {
        _playlist.value.getOrNull(_index.value - 1)?.let { open(it.uri.toUri()) }
    }

    /** Stops playback and drops the engine. Called when the player screen leaves composition. */
    fun stop() {
        saveProgress()
        loadJob?.cancel()
        loadJob = null
        controller?.release()
        controller = null
        _controllerFlow.value = null
        _playerState.value = PlayerState()
        lastAutoSaveMs = 0L
    }

    override fun onCleared() {
        stop()
    }

    private fun seekRelative(deltaMs: Long) {
        val state = _playerState.value
        val max = if (state.durationMs > 0L) state.durationMs else Long.MAX_VALUE
        controller?.seekTo((state.positionMs + deltaMs).coerceIn(0L, max))
    }

    /** Sibling videos in the same folder become the next/previous queue. Content URIs have no path. */
    private fun resolvePlaylist(uri: Uri) {
        val parent = uri.path?.let(::File)?.parent ?: return
        viewModelScope.launch {
            val videos = runCatching { storageRepository.listDirectory(parent).videos }
                .getOrDefault(emptyList())
            _playlist.value = videos
            _index.value = videos.indexOfFirst { it.uri == uri.toString() }
        }
    }

    private fun createController(): PlayerController? =
        try {
            controllerFactory.create(PlayerConfig()).also { created ->
                controller = created
                _controllerFlow.value = created
                viewModelScope.launch {
                    created.state.collect { state ->
                        _playerState.value = state
                        maybeAutoSave(state)
                    }
                }
            }
        } catch (e: Exception) {
            _fatalError.value = PlayerError.Engine(e.message)
            null
        }

    /** Persists progress while playing so a kill or a back press doesn't lose the position. */
    private fun maybeAutoSave(state: PlayerState) {
        if (state.status == PlayerState.Status.Ended) {
            saveProgress()
            return
        }
        if (state.status != PlayerState.Status.Playing || state.durationMs <= 0L) return
        if (state.positionMs - lastAutoSaveMs >= AUTO_SAVE_INTERVAL_MS) {
            lastAutoSaveMs = state.positionMs
            saveProgress()
        }
    }

    private fun saveProgress() {
        val uri = _videoUri.value?.toString() ?: return
        val state = _playerState.value
        if (state.status == PlayerState.Status.Ended) {
            clearProgress()
            return
        }
        if (state.durationMs <= 0L) return
        viewModelScope.launch { runCatching { progressRepository.save(uri, state.positionMs, state.durationMs) } }
    }

    private fun clearProgress() {
        val uri = _videoUri.value?.toString() ?: return
        viewModelScope.launch { runCatching { progressRepository.clear(uri) } }
    }

    companion object {
        private const val KEY_VIDEO_URI = "video_uri"
        private const val SKIP_STEP_MS = 10_000L
        private const val AUTO_SAVE_INTERVAL_MS = 10_000L
    }
}
