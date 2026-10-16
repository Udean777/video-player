package app.subtle.player.engine.vlc

import android.view.ViewGroup
import app.subtle.player.core.common.PlayerDispatcher
import app.subtle.player.core.player.MediaSource
import app.subtle.player.core.player.PlayerConfig
import app.subtle.player.core.player.PlayerController
import app.subtle.player.core.player.PlayerControllerFactory
import app.subtle.player.core.player.PlayerError
import app.subtle.player.core.player.PlayerState
import app.subtle.player.core.player.SubtitleSource
import app.subtle.player.core.player.Track
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.interfaces.IMedia
import org.videolan.libvlc.util.VLCVideoLayout
import android.content.Context as AndroidContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * libVLC implementation of [PlayerController].
 *
 * All [MediaPlayer] calls run on [playerDispatcher]. Engine callbacks arrive on
 * libVLC threads and only touch [_state] or hop onto the scope.
 */
internal class VlcPlayerController(
    private val libVlc: LibVLC,
    private val player: MediaPlayer,
    private val playerDispatcher: CoroutineDispatcher,
) : PlayerController {

    private val scope = CoroutineScope(SupervisorJob() + playerDispatcher)
    private val _state = MutableStateFlow(PlayerState())
    override val state: StateFlow<PlayerState> = _state.asStateFlow()

    private var pendingStartMs: Long = 0L
    private var released: Boolean = false
    private var videoLayout: VLCVideoLayout? = null

    private val listener =
        MediaPlayer.EventListener { event ->
            when (event.type) {
                MediaPlayer.Event.Opening -> _state.update { it.copy(status = PlayerState.Status.Opening) }
                MediaPlayer.Event.Buffering -> _state.update {
                    // libVLC also reports buffering completion (100) around playback start; treating
                    // that as "playing" keeps the spinner from sticking over a running video.
                    when {
                        event.buffering < 100f -> it.copy(status = PlayerState.Status.Buffering)
                        it.status == PlayerState.Status.Buffering -> it.copy(status = PlayerState.Status.Playing)
                        else -> it
                    }
                }
                MediaPlayer.Event.Playing -> {
                    _state.update { it.copy(status = PlayerState.Status.Playing, error = null) }
                    scope.launch { refreshTracksAndPosition() }
                }
                MediaPlayer.Event.Paused -> _state.update { it.copy(status = PlayerState.Status.Paused) }
                MediaPlayer.Event.Stopped -> _state.update { it.copy(status = PlayerState.Status.Idle) }
                MediaPlayer.Event.EndReached -> _state.update { it.copy(status = PlayerState.Status.Ended) }
                MediaPlayer.Event.EncounteredError ->
                    _state.update { it.copy(status = PlayerState.Status.Error, error = PlayerError.Engine(null)) }
                MediaPlayer.Event.TimeChanged ->
                    scope.launch {
                        val time = runCatching { player.time }.getOrDefault(-1L)
                        if (time >= 0L) _state.update { it.copy(positionMs = time) }
                    }
                MediaPlayer.Event.LengthChanged ->
                    scope.launch {
                        val length = runCatching { player.length }.getOrDefault(-1L)
                        if (length > 0L) _state.update { it.copy(durationMs = length) }
                    }
                MediaPlayer.Event.SeekableChanged,
                MediaPlayer.Event.ESAdded,
                MediaPlayer.Event.ESDeleted,
                MediaPlayer.Event.ESSelected,
                -> scope.launch { refreshTracksAndPosition() }
            }
        }

    init {
        player.setEventListener(listener)
    }

    override suspend fun load(source: MediaSource, startPositionMs: Long) {
        withContext(playerDispatcher) {
            if (released) return@withContext
            pendingStartMs = startPositionMs.coerceAtLeast(0L)
            _state.update { PlayerState(status = PlayerState.Status.Opening) }
            val media =
                when (source) {
                    is MediaSource.Content -> Media(libVlc, source.uri)
                }
            runCatching {
                player.media = media
                player.play()
            }.onFailure {
                _state.update {
                    it.copy(status = PlayerState.Status.Error, error = PlayerError.FileUnavailable)
                }
            }
            media.release()
        }
    }

    override fun play() {
        scope.launch { runCatching { player.play() } }
    }

    override fun pause() {
        scope.launch { runCatching { player.pause() } }
    }

    override fun seekTo(positionMs: Long) {
        scope.launch {
            runCatching {
                player.time = positionMs.coerceAtLeast(0L)
                _state.update { it.copy(positionMs = positionMs.coerceAtLeast(0L)) }
            }
        }
    }

    override fun setSpeed(rate: Float) {
        val clamped = rate.coerceIn(0.25f, 4f)
        scope.launch {
            runCatching {
                player.rate = clamped
                _state.update { it.copy(speed = clamped) }
            }
        }
    }

    override fun selectAudioTrack(id: Int) {
        scope.launch {
            runCatching {
                player.audioTrack = id
                refreshTracksAndPosition()
            }
        }
    }

    override fun selectSubtitleTrack(id: Int) {
        scope.launch {
            runCatching {
                player.spuTrack = id
                refreshTracksAndPosition()
            }
        }
    }

    override suspend fun addExternalSubtitle(source: SubtitleSource, select: Boolean) {
        withContext(playerDispatcher) {
            runCatching {
                val uri =
                    when (source) {
                        is SubtitleSource.Content -> source.uri
                    }
                player.addSlave(IMedia.Slave.Type.Subtitle, uri, select)
                refreshTracksAndPosition()
            }
        }
    }

    override fun setSubtitleDelay(delayMs: Long) {
        scope.launch {
            runCatching {
                player.setSpuDelay(delayMs * 1_000L)
                _state.update { it.copy(subtitleDelayMs = delayMs) }
            }
        }
    }

    override fun setAudioDelay(delayMs: Long) {
        scope.launch {
            runCatching {
                player.setAudioDelay(delayMs * 1_000L)
                _state.update { it.copy(audioDelayMs = delayMs) }
            }
        }
    }

    override fun attachSurface(container: ViewGroup) {
        val videoLayout = VLCVideoLayout(container.context)
        this.videoLayout = videoLayout
        container.addView(
            videoLayout,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            ),
        )
        // The layout (not a bare SurfaceView) lets libVLC size and centre the video itself.
        player.attachViews(videoLayout, null, false, false)
    }

    override fun detachSurface() {
        runCatching { player.detachViews() }
        videoLayout?.let { layout -> (layout.parent as? ViewGroup)?.removeView(layout) }
        videoLayout = null
    }

    override fun release() {
        scope.launch {
            released = true
            runCatching {
                player.stop()
                player.detachViews()
                player.release()
                libVlc.release()
            }
            scope.cancel()
        }
    }

    private suspend fun refreshTracksAndPosition() {
        if (released) return
        val audio = runCatching { player.audioTracks?.map { Track(it.id, it.name.orEmpty()) }.orEmpty() }
            .getOrDefault(emptyList())
        val spu = runCatching { player.spuTracks?.map { Track(it.id, it.name.orEmpty()) }.orEmpty() }
            .getOrDefault(emptyList())
        val selectedAudio = runCatching { player.audioTrack }.getOrDefault(Track.DISABLED_ID)
        val selectedSpu = runCatching { player.spuTrack }.getOrDefault(Track.DISABLED_ID)
        val seekable = runCatching { player.isSeekable }.getOrDefault(false)
        val length = runCatching { player.length }.getOrDefault(-1L).coerceAtLeast(0L)
        _state.update {
            it.copy(
                audioTracks = audio,
                subtitleTracks = spu,
                selectedAudioTrackId = selectedAudio.takeIf { id -> id >= 0 },
                selectedSubtitleTrackId = selectedSpu.takeIf { id -> id >= 0 },
                isSeekable = seekable,
                durationMs = if (length > 0L) length else it.durationMs,
            )
        }
        maybeApplyPendingPosition(seekable)
    }

    private suspend fun maybeApplyPendingPosition(seekable: Boolean) {
        val pending = pendingStartMs
        if (pending <= 0L || !seekable || released) return
        pendingStartMs = 0L
        runCatching { player.time = pending }
        _state.update { it.copy(positionMs = pending) }
    }
}

@Singleton
class VlcPlayerControllerFactory @Inject constructor(
    @ApplicationContext private val context: AndroidContext,
    @PlayerDispatcher private val playerDispatcher: CoroutineDispatcher,
) : PlayerControllerFactory {
    override fun create(config: PlayerConfig): PlayerController {
        val libVlc = LibVLC(context, buildVlcArgs(config))
        val mediaPlayer = MediaPlayer(libVlc)
        return VlcPlayerController(libVlc, mediaPlayer, playerDispatcher)
    }
}
