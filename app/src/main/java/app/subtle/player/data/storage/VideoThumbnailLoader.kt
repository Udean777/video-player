package app.subtle.player.data.storage

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.util.LruCache
import androidx.core.net.toUri
import app.subtle.player.core.common.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Grabs a single frame per video and keeps it in memory.
 *
 * ponytail: memory-only cache, regenerated each launch. Add a `cacheDir/thumbs` disk layer
 * if first-scroll thumbnail cost ever shows up on real libraries.
 */
@Singleton
class VideoThumbnailLoader @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val io: CoroutineDispatcher,
) {
    private val cache = LruCache<String, Bitmap>(120)

    suspend fun load(uri: String): Bitmap? = withContext(io) {
        cache.get(uri)?.let { return@withContext it }
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri.toUri())
            val frame = retriever.getFrameAtTime(FRAME_US, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            frame?.also { cache.put(uri, it) }
        } catch (_: Exception) {
            null
        } finally {
            retriever.release()
        }
    }

    private companion object {
        const val FRAME_US = 1_000_000L
    }
}
