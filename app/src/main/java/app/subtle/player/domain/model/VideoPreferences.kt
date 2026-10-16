package app.subtle.player.domain.model

/** Per-video playback preferences, remembered across sessions. */
data class VideoPreferences(
    val subtitleDelayMs: Long = 0L,
    val audioDelayMs: Long = 0L,
    val hasHardSubs: Boolean = false,
    val charsetOverride: String? = null,
)
