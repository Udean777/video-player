package app.subtle.player.data.repository

import android.content.Context
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PreferencesPlaybackProgressRepositoryTest {

    private val context: Context get() = RuntimeEnvironment.getApplication()

    @Test
    fun saveStampsLastPlayedAndReloads() = runTest {
        val repository = PreferencesPlaybackProgressRepository(context)

        repository.save(URI, positionMs = 1_000L, durationMs = 2_000L)
        val stored = repository.progress.first().getValue(URI)

        assertEquals(1_000L, stored.positionMs)
        assertEquals(2_000L, stored.durationMs)
        assertTrue(stored.lastPlayedMs > 0L)

        val reloaded = PreferencesPlaybackProgressRepository(context).progress.first().getValue(URI)
        assertEquals(stored, reloaded)
    }

    @Test
    fun decodesLegacyTwoFieldEntry() = runTest {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(URI, "5000,60000")
            .commit()

        val stored = PreferencesPlaybackProgressRepository(context).progress.first().getValue(URI)

        assertEquals(5_000L, stored.positionMs)
        assertEquals(60_000L, stored.durationMs)
        assertEquals(0L, stored.lastPlayedMs)
    }

    private companion object {
        const val URI = "file:///movies/a.mp4"
        const val PREFS_NAME = "playback_progress"
    }
}
