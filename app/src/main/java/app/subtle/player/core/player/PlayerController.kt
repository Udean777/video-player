package app.subtle.player.core.player

import app.subtle.player.domain.model.SubtitleStyle
import kotlinx.coroutines.flow.StateFlow

/**
 * Engine-agnostic playback contract. The UI and ViewModels never see libVLC types;
 * swapping engines means implementing this interface in `engine/` and rebinding `di/`.
 */
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

fun interface PlayerControllerFactory {
    fun create(config: PlayerConfig): PlayerController
}

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

sealed interface MediaSource {
    data class Content(val uri: android.net.Uri) : MediaSource
}

sealed interface SubtitleSource {
    data class Content(val uri: android.net.Uri, val displayName: String) : SubtitleSource
}

sealed interface PlayerError {
    data object UnsupportedFormat : PlayerError
    data object FileUnavailable : PlayerError
    data class Engine(val message: String?) : PlayerError
}
