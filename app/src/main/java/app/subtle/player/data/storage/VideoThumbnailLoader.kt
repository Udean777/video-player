package app.subtle.player.data.storage

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.os.Build
import android.util.LruCache
import androidx.core.net.toUri
import app.subtle.player.core.common.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Grabs one downscaled frame per video and keeps it in memory.
 *
 * ponytail: memory-only cache, regenerated each launch. Add a `cacheDir/thumbs` disk layer
 * if first-scroll thumbnail cost ever shows up on real libraries.
 */
@Singleton
class VideoThumbnailLoader @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val io: CoroutineDispatcher,
) {
    // Budgeted in bytes: a full-resolution frame is megabytes, so a count cap risks OOM.
    private val cache = object : LruCache<String, Bitmap>(cacheBudgetKb()) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.allocationByteCount / 1024
    }

    // A fast scroll would otherwise start one decoder per visible row at the same time.
    private val permits = Semaphore(MAX_PARALLEL_DECODES)

    suspend fun load(uri: String): Bitmap? = withContext(io) {
        cache.get(uri)?.let { return@withContext it }
        decode(uri)?.also { cache.put(uri, it) }
    }

    private suspend fun decode(uri: String): Bitmap? = permits.withPermit {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri.toUri())
            scaledFrame(retriever)
        } catch (_: Exception) {
            null
        } finally {
            retriever.release()
        }
    }

    /** Decodes straight to thumbnail size where the platform can, so no full frame is allocated. */
    private fun scaledFrame(retriever: MediaMetadataRetriever): Bitmap? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            retriever.getScaledFrameAtTime(FRAME_US, OPTION, THUMB_WIDTH, THUMB_HEIGHT)
        } else {
            retriever.getFrameAtTime(FRAME_US, OPTION)
                ?.let { Bitmap.createScaledBitmap(it, THUMB_WIDTH, THUMB_HEIGHT, true) }
        }

    private companion object {
        const val FRAME_US = 1_000_000L

        /** Seeking to the closest key frame is the cheap option; OPTION_CLOSEST decodes a whole GOP. */
        const val OPTION = MediaMetadataRetriever.OPTION_CLOSEST_SYNC
        const val THUMB_WIDTH = 320
        const val THUMB_HEIGHT = 180
        const val MAX_PARALLEL_DECODES = 3

        /** An eighth of the heap holds a library's worth of 320x180 frames without crowding playback. */
        fun cacheBudgetKb(): Int = (Runtime.getRuntime().maxMemory() / 1024 / 8).toInt()
    }
}
