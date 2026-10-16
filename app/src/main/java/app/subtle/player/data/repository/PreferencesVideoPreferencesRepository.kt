package app.subtle.player.data.repository

import android.content.Context
import androidx.core.content.edit
import app.subtle.player.domain.model.VideoPreferences
import app.subtle.player.domain.repository.VideoPreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ponytail: SharedPreferences keyed by URI, one pipe-separated record each. Fine until the indexed
 * (Room) pass; move to `VideoPreferenceEntity` then.
 */
@Singleton
class PreferencesVideoPreferencesRepository @Inject constructor(
    @ApplicationContext context: Context,
) : VideoPreferencesRepository {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override suspend fun get(videoUri: String): VideoPreferences =
        prefs.getString(videoUri, null)?.let(::decode) ?: VideoPreferences()

    override suspend fun save(videoUri: String, preferences: VideoPreferences) {
        if (preferences == VideoPreferences()) {
            prefs.edit { remove(videoUri) }
        } else {
            prefs.edit { putString(videoUri, encode(preferences)) }
        }
    }

    private fun encode(p: VideoPreferences): String =
        listOf(p.subtitleDelayMs, p.audioDelayMs, if (p.hasHardSubs) 1 else 0, p.charsetOverride ?: "")
            .joinToString("|")

    private fun decode(raw: String): VideoPreferences {
        val parts = raw.split("|")
        if (parts.size < 4) return VideoPreferences()
        return VideoPreferences(
            subtitleDelayMs = parts[0].toLongOrNull() ?: 0L,
            audioDelayMs = parts[1].toLongOrNull() ?: 0L,
            hasHardSubs = parts[2] == "1",
            charsetOverride = parts[3].takeIf { it.isNotBlank() },
        )
    }

    private companion object {
        const val PREFS_NAME = "video_preferences"
    }
}
