package app.subtle.player.core.player

import android.content.Context
import androidx.core.content.ContextCompat
import app.subtle.player.PlaybackService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** What the background notification shows. */
data class NowPlaying(
    val title: String = "",
    val isPlaying: Boolean = false,
    val hasPrevious: Boolean = false,
    val hasNext: Boolean = false,
)

/**
 * Process-wide bridge between the player (owned by `PlayerViewModel`) and the foreground
 * [PlaybackService]. The screen keeps ownership of the engine; the service only keeps the process
 * alive and exposes controls while the player is active.
 */
@Singleton
class PlaybackHolder @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private val _nowPlaying = MutableStateFlow(NowPlaying())
    val nowPlaying: StateFlow<NowPlaying> = _nowPlaying.asStateFlow()

    var controller: PlayerController? = null
        private set

    var onTogglePlayPause: (() -> Unit)? = null
    var onNext: (() -> Unit)? = null
    var onPrevious: (() -> Unit)? = null

    fun attach(controller: PlayerController, title: String) {
        this.controller = controller
        _nowPlaying.value = NowPlaying(title = title)
        runCatching { ContextCompat.startForegroundService(context, PlaybackService.intent(context)) }
    }

    fun update(
        title: String? = null,
        isPlaying: Boolean? = null,
        hasPrevious: Boolean? = null,
        hasNext: Boolean? = null,
    ) {
        _nowPlaying.update {
            it.copy(
                title = title ?: it.title,
                isPlaying = isPlaying ?: it.isPlaying,
                hasPrevious = hasPrevious ?: it.hasPrevious,
                hasNext = hasNext ?: it.hasNext,
            )
        }
    }

    fun togglePlayPause() {
        onTogglePlayPause?.invoke()
    }

    fun detach() {
        controller = null
        onTogglePlayPause = null
        onNext = null
        onPrevious = null
        _nowPlaying.value = NowPlaying()
        runCatching { context.stopService(PlaybackService.intent(context)) }
    }
}
