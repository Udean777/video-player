package app.subtle.player.domain.model

/**
 * Saved playback position for a video. [positionMs] is where playback stopped,
 * [durationMs] the total length (0 when unknown), [lastPlayedMs] when it was last saved.
 */
data class PlaybackProgress(
    val positionMs: Long,
    val durationMs: Long,
    val lastPlayedMs: Long = 0L,
) {
    val fraction: Float
        get() = if (durationMs <= 0L) 0f else (positionMs.toFloat() / durationMs).coerceIn(0f, 1f)

    /** Resume point for the next play. Near the start or effectively finished restarts from 0. */
    fun resumePositionMs(): Long {
        if (durationMs <= 0L) return 0L
        if (positionMs < MIN_RESUME_MS) return 0L
        if (positionMs >= durationMs - END_TOLERANCE_MS) return 0L
        return positionMs
    }

    companion object {
        const val MIN_RESUME_MS = 5_000L
        const val END_TOLERANCE_MS = 5_000L
    }
}
