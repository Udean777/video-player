package app.subtle.player.domain.repository

import app.subtle.player.domain.model.PlaybackProgress
import kotlinx.coroutines.flow.Flow

interface PlaybackProgressRepository {

    /** Per-URI progress, kept in sync as the player saves. */
    val progress: Flow<Map<String, PlaybackProgress>>

    suspend fun get(uri: String): PlaybackProgress?

    suspend fun save(uri: String, positionMs: Long, durationMs: Long)

    /** Drops the record, e.g. once a video has been watched to the end. */
    suspend fun clear(uri: String)
}
