package app.subtle.player.feature.player

import android.net.Uri
import app.subtle.player.core.player.PlayerError
import app.subtle.player.core.player.PlayerState

data class PlayerUiState(
    val videoUri: Uri? = null,
    val player: PlayerState = PlayerState(),
    val fatalError: PlayerError? = null,
    val hasPrevious: Boolean = false,
    val hasNext: Boolean = false,
) {
    val error: PlayerError?
        get() = fatalError ?: player.error
}
