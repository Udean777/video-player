package app.subtle.player.feature.player

import android.net.Uri
import app.subtle.player.core.player.PlayerError
import app.subtle.player.core.player.PlayerState
import app.subtle.player.domain.model.SubtitleFile
import app.subtle.player.domain.model.VideoPreferences

data class PlayerUiState(
    val videoUri: Uri? = null,
    val player: PlayerState = PlayerState(),
    val fatalError: PlayerError? = null,
    val hasPrevious: Boolean = false,
    val hasNext: Boolean = false,
    val externalSubtitles: List<SubtitleFile> = emptyList(),
    val videoPreferences: VideoPreferences = VideoPreferences(),
) {
    val error: PlayerError?
        get() = fatalError ?: player.error
}
