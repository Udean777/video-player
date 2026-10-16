package app.subtle.player.feature.common

import app.subtle.player.domain.model.PlaybackProgress
import app.subtle.player.domain.repository.PlaybackProgressRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakePlaybackProgressRepository : PlaybackProgressRepository {
    val saved = mutableMapOf<String, PlaybackProgress>()
    private val _progress = MutableStateFlow<Map<String, PlaybackProgress>>(emptyMap())

    override val progress: Flow<Map<String, PlaybackProgress>> = _progress

    override suspend fun get(uri: String): PlaybackProgress? = saved[uri]

    override suspend fun save(uri: String, positionMs: Long, durationMs: Long) {
        val entry = PlaybackProgress(positionMs, durationMs)
        saved[uri] = entry
        _progress.value = saved.toMap()
    }

    override suspend fun clear(uri: String) {
        saved.remove(uri)
        _progress.value = saved.toMap()
    }
}
