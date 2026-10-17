package app.subtle.player.data.repository

import android.content.Context
import androidx.core.content.edit
import app.subtle.player.domain.model.PlaybackProgress
import app.subtle.player.domain.repository.PlaybackProgressRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * ponytail: SharedPreferences keyed by URI ("positionMs,durationMs,lastPlayedMs"). No Room/DataStore
 * wiring needed; swap for a database if the library ever grows large enough to make getAll() slow.
 */
@Singleton
class PreferencesPlaybackProgressRepository @Inject constructor(
    @ApplicationContext context: Context,
) : PlaybackProgressRepository {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val _progress = MutableStateFlow(readAll())

    override val progress: Flow<Map<String, PlaybackProgress>> = _progress.asStateFlow()

    override suspend fun get(uri: String): PlaybackProgress? = _progress.value[uri]

    override suspend fun save(uri: String, positionMs: Long, durationMs: Long) {
        val entry = PlaybackProgress(positionMs, durationMs, System.currentTimeMillis())
        prefs.edit { putString(uri, "${entry.positionMs},${entry.durationMs},${entry.lastPlayedMs}") }
        _progress.update { it + (uri to entry) }
    }

    override suspend fun clear(uri: String) {
        prefs.edit { remove(uri) }
        _progress.update { it - uri }
    }

    private fun readAll(): Map<String, PlaybackProgress> =
        prefs.all.mapNotNull { (uri, value) -> decode(value as? String)?.let { uri to it } }.toMap()

    private fun decode(value: String?): PlaybackProgress? {
        val parts = value?.split(',') ?: return null
        if (parts.size < 2) return null
        val position = parts[0].toLongOrNull() ?: return null
        val duration = parts[1].toLongOrNull() ?: return null
        val lastPlayed = parts.getOrNull(2)?.toLongOrNull() ?: 0L
        return PlaybackProgress(position, duration, lastPlayed)
    }

    private companion object {
        const val PREFS_NAME = "playback_progress"
    }
}
