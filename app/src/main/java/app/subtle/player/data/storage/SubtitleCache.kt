package app.subtle.player.data.storage

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import app.subtle.player.core.common.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileNotFoundException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/**
 * Copies an external subtitle into `cacheDir/subs/` so libVLC can read it as a `file://` URI.
 * Text formats are normalized to UTF-8; image formats (`.sub`, `.idx`) are copied verbatim.
 * The cache is disposable.
 */
@Singleton
class SubtitleCache @Inject constructor(
    @ApplicationContext private val context: Context,
    private val normalizer: CharsetNormalizer,
    @IoDispatcher private val io: CoroutineDispatcher,
) {

    suspend fun prepare(sourceUri: String, displayName: String, charsetOverride: String?): String =
        withContext(io) {
            val source = sourceUri.toUri()
            val dir = File(context.cacheDir, "subs").apply { mkdirs() }
            val safeName = displayName.replace(Regex("[^A-Za-z0-9._-]"), "_")
            val target = File(dir, "${sourceUri.hashCode()}_$safeName")
            val bytes = context.contentResolver.openInputStream(source)?.use { it.readBytes() }
                ?: throw FileNotFoundException(sourceUri)
            require(bytes.size <= MAX_BYTES) { "subtitle too large" }
            val isText = displayName.substringAfterLast('.', "").lowercase() in TEXT_EXTENSIONS
            target.writeBytes(if (isText) normalizer.toUtf8(bytes, charsetOverride) else bytes)
            Uri.fromFile(target).toString()
        }

    private companion object {
        const val MAX_BYTES = 10 * 1024 * 1024
        val TEXT_EXTENSIONS = setOf("srt", "ass", "ssa", "vtt", "smi", "txt")
    }
}
