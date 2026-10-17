package app.subtle.player.feature.player

import android.net.Uri
import androidx.core.net.toUri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.subtle.player.core.common.PlayerDispatcher
import app.subtle.player.core.player.AspectRatio
import app.subtle.player.core.player.MediaSource
import app.subtle.player.core.player.PlayerConfig
import app.subtle.player.core.player.PlayerController
import app.subtle.player.core.player.PlayerControllerFactory
import app.subtle.player.core.player.PlayerError
import app.subtle.player.core.player.PlayerState
import app.subtle.player.core.player.PlaybackHolder
import app.subtle.player.core.player.SubtitleSource
import app.subtle.player.core.player.Track
import app.subtle.player.domain.model.SubtitleFile
import app.subtle.player.domain.model.SubtitleLanguage
import app.subtle.player.domain.model.SubtitleStyle
import app.subtle.player.domain.model.Video
import app.subtle.player.domain.model.VideoPreferences
import app.subtle.player.domain.repository.PlaybackProgressRepository
import app.subtle.player.domain.repository.SettingsRepository
import app.subtle.player.domain.repository.StorageRepository
import app.subtle.player.domain.repository.SubtitleRepository
import app.subtle.player.domain.repository.VideoPreferencesRepository
import app.subtle.player.domain.usecase.SubtitleLanguageMatcher
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val controllerFactory: PlayerControllerFactory,
    private val storageRepository: StorageRepository,
    private val progressRepository: PlaybackProgressRepository,
    private val subtitleRepository: SubtitleRepository,
    private val preferencesRepository: VideoPreferencesRepository,
    private val settingsRepository: SettingsRepository,
    private val playbackHolder: PlaybackHolder,
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
    private val _externalSubtitles = MutableStateFlow<List<SubtitleFile>>(emptyList())
    private val _videoPreferences = MutableStateFlow(VideoPreferences())

    val uiState: StateFlow<PlayerUiState> =
        combine(
            combine(_videoUri, _playerState, _fatalError, _playlist, _index) { uri, player, fatal, playlist, index ->
                PlayerUiState(
                    videoUri = uri,
                    player = player,
                    fatalError = fatal,
                    hasPrevious = index > 0,
                    hasNext = index >= 0 && index < playlist.lastIndex,
                )
            },
            _externalSubtitles,
            _videoPreferences,
        ) { base, subtitles, prefs ->
            base.copy(externalSubtitles = subtitles, videoPreferences = prefs)
        }.stateIn(viewModelScope, SharingStarted.Eagerly, PlayerUiState())

    private var controller: PlayerController? = null
    private var loadJob: Job? = null
    private var lastAutoSaveMs = 0L
    private var delaysApplied = false
    private var hardSubsApplied = false
    private var languageApplied = false
    private var subtitlesDiscovered = false
    private var forceSoftwareDecoding = false
    private var subtitleStyle = SubtitleStyle()
    private var subtitleLanguage = SubtitleLanguage.None
    private var autoPlayNext = true
    private var endedHandled = false
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
        _externalSubtitles.value = emptyList()
        _videoPreferences.value = VideoPreferences()
        delaysApplied = false
        hardSubsApplied = false
        languageApplied = false
        subtitlesDiscovered = false
        endedHandled = false
        loadJob?.cancel()
        // LibVLC construction and the first decode are heavy; keep them off the main thread.
        loadJob = viewModelScope.launch(playerDispatcher) {
            runCatching { settingsRepository.settings.first() }.onSuccess {
                forceSoftwareDecoding = it.forceSoftwareDecoding
                subtitleStyle = it.subtitleStyle
                subtitleLanguage = it.subtitleLanguage
                autoPlayNext = it.autoPlayNext
            }
            val active = controller ?: createController() ?: return@launch
            val startMs = runCatching { progressRepository.get(uri.toString())?.resumePositionMs() ?: 0L }
                .getOrDefault(0L)
            runCatching { preferencesRepository.get(uri.toString()) }.onSuccess { _videoPreferences.value = it }
            try {
                active.load(MediaSource.Content(uri), startMs)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _fatalError.value = PlayerError.Engine(e.message)
            }
            resolvePlaylist(uri)
            discoverSubtitles(uri.toString())
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

    /** [Track.DISABLED_ID] turns subtitles off. */
    fun selectSubtitleTrack(id: Int) {
        controller?.selectSubtitleTrack(id)
    }

    fun selectAudioTrack(id: Int) {
        controller?.selectAudioTrack(id)
    }

    fun setSpeed(rate: Float) {
        controller?.setSpeed(rate)
    }

    fun setVolume(level: Int) {
        controller?.setVolume(level)
    }

    fun setAspectRatio(ratio: AspectRatio) {
        controller?.setAspectRatio(ratio)
    }

    fun adjustSubtitleDelay(deltaMs: Long) {
        val next = (_playerState.value.subtitleDelayMs + deltaMs).coerceIn(-MAX_DELAY_MS, MAX_DELAY_MS)
        controller?.setSubtitleDelay(next)
        persistPreferences(_videoPreferences.value.copy(subtitleDelayMs = next))
    }

    fun resetSubtitleDelay() {
        controller?.setSubtitleDelay(0L)
        persistPreferences(_videoPreferences.value.copy(subtitleDelayMs = 0L))
    }

    fun setHardSubs(enabled: Boolean) {
        persistPreferences(_videoPreferences.value.copy(hasHardSubs = enabled))
        if (enabled) controller?.selectSubtitleTrack(Track.DISABLED_ID)
    }

    fun setCharsetOverride(charset: String?) {
        persistPreferences(_videoPreferences.value.copy(charsetOverride = charset))
    }

    fun adjustAudioDelay(deltaMs: Long) {
        val next = (_playerState.value.audioDelayMs + deltaMs).coerceIn(-MAX_DELAY_MS, MAX_DELAY_MS)
        controller?.setAudioDelay(next)
        persistPreferences(_videoPreferences.value.copy(audioDelayMs = next))
    }

    fun resetAudioDelay() {
        controller?.setAudioDelay(0L)
        persistPreferences(_videoPreferences.value.copy(audioDelayMs = 0L))
    }

    /** Adds an external file (discovered or picked) to the current session and selects it. */
    fun loadSubtitle(subtitle: SubtitleFile) {
        viewModelScope.launch {
            val prepared = runCatching {
                subtitleRepository.prepareForPlayback(subtitle, _videoPreferences.value.charsetOverride)
            }.getOrNull() ?: return@launch
            controller?.addExternalSubtitle(
                SubtitleSource.Content(prepared.toUri(), subtitle.displayName),
                select = true,
            )
        }
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
        playbackHolder.detach()
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
            syncNowPlaying()
        }
    }

    private fun discoverSubtitles(videoUri: String) {
        viewModelScope.launch {
            val subtitles = runCatching { storageRepository.subtitlesFor(videoUri) }
                .getOrDefault(emptyList())
            _externalSubtitles.value = subtitles
            subtitlesDiscovered = true
            if (subtitleLanguage != SubtitleLanguage.None) {
                subtitles.firstOrNull { SubtitleLanguageMatcher.matches(subtitleLanguage, it.languageHint) }
                    ?.let {
                        loadSubtitle(it)
                        languageApplied = true
                    }
            }
        }
    }

    private fun persistPreferences(preferences: VideoPreferences) {
        _videoPreferences.value = preferences
        val uri = _videoUri.value?.toString() ?: return
        viewModelScope.launch { runCatching { preferencesRepository.save(uri, preferences) } }
    }

    private fun createController(): PlayerController? =
        try {
            controllerFactory.create(
                PlayerConfig(forceSoftwareDecoding = forceSoftwareDecoding, subtitleStyle = subtitleStyle),
            ).also { created ->
                controller = created
                _controllerFlow.value = created
                playbackHolder.attach(created, title())
                playbackHolder.onTogglePlayPause = {
                    if (_playerState.value.status == PlayerState.Status.Playing) pause() else play()
                }
                playbackHolder.onNext = ::next
                playbackHolder.onPrevious = ::previous
                viewModelScope.launch {
                    created.state.collect { state ->
                        _playerState.value = state
                        syncNowPlaying()
                        maybeAutoSave(state)
                        applyInitialPreferences(state)
                    }
                }
            }
        } catch (e: Exception) {
            _fatalError.value = PlayerError.Engine(e.message)
            null
        }

    private fun title(): String =
        _videoUri.value?.lastPathSegment?.substringAfterLast('/') ?: _videoUri.value?.toString().orEmpty()

    private fun syncNowPlaying() {
        playbackHolder.update(
            title = title(),
            isPlaying = _playerState.value.status == PlayerState.Status.Playing,
            hasPrevious = _index.value > 0,
            hasNext = _index.value in 0..<_playlist.value.size - 1,
        )
    }

    /** Applies the saved delay and hard-subtitle policy once their prerequisites exist. */
    private fun applyInitialPreferences(state: PlayerState) {
        val prefs = _videoPreferences.value
        if (!delaysApplied && state.status == PlayerState.Status.Playing) {
            delaysApplied = true
            controller?.setSubtitleDelay(prefs.subtitleDelayMs)
            controller?.setAudioDelay(prefs.audioDelayMs)
        }
        if (!hardSubsApplied && prefs.hasHardSubs && state.subtitleTracks.isNotEmpty()) {
            hardSubsApplied = true
            controller?.selectSubtitleTrack(Track.DISABLED_ID)
        }
        if (subtitlesDiscovered && !languageApplied && !prefs.hasHardSubs &&
            subtitleLanguage != SubtitleLanguage.None && state.subtitleTracks.isNotEmpty()
        ) {
            state.subtitleTracks
                .firstOrNull { SubtitleLanguageMatcher.matches(subtitleLanguage, it.name) }
                ?.let {
                    languageApplied = true
                    controller?.selectSubtitleTrack(it.id)
                }
        }
    }

    /** Persists progress while playing so a kill or a back press doesn't lose the position. */
    private fun maybeAutoSave(state: PlayerState) {
        if (state.status == PlayerState.Status.Ended) {
            if (!endedHandled) {
                endedHandled = true
                saveProgress()
                if (autoPlayNext && _index.value < _playlist.value.lastIndex) next()
            }
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
        private const val MAX_DELAY_MS = 60_000L
    }
}
