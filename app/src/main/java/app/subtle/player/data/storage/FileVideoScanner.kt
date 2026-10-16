package app.subtle.player.data.storage

import android.net.Uri
import android.os.Environment
import app.subtle.player.core.common.IoDispatcher
import app.subtle.player.domain.model.DirectoryContent
import app.subtle.player.domain.model.FolderEntry
import app.subtle.player.domain.model.Video
import app.subtle.player.domain.model.isVideoFile
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileVideoScanner @Inject constructor(
    @IoDispatcher private val io: CoroutineDispatcher,
) {

    suspend fun allVideos(): List<Video> = withContext(io) {
        val found = mutableListOf<Video>()
        collectVideos(root(), found)
        found.sortedBy { it.displayName.lowercase() }
    }

    suspend fun listDirectory(path: String?): DirectoryContent = withContext(io) {
        val dir = path?.let(::File) ?: root()
        val children = dir.listFiles().orEmpty()

        val folders = children
            .filter { it.isBrowsable() }
            .mapNotNull { sub ->
                val count = countVideos(sub)
                if (count > 0) FolderEntry(sub.name, sub.absolutePath, count) else null
            }
            .sortedBy { it.name.lowercase() }

        val videos = children
            .filter { it.isFile && !it.isHidden && isVideoFile(null, it.name) }
            .map { it.toVideo() }
            .sortedBy { it.displayName.lowercase() }

        DirectoryContent(
            displayName = dir.name.ifEmpty { dir.absolutePath },
            folders = folders,
            videos = videos,
        )
    }

    @Suppress("DEPRECATION")
    private fun root(): File = Environment.getExternalStorageDirectory()

    // ponytail: recursive scan on every directory open. Fine for typical libraries;
    // move to a background index (Room) if a deep tree ever feels slow.
    private fun collectVideos(dir: File, out: MutableList<Video>) {
        dir.listFiles()?.forEach { child ->
            when {
                child.isBrowsable() -> collectVideos(child, out)
                child.isFile && !child.isHidden && isVideoFile(null, child.name) -> out.add(child.toVideo())
            }
        }
    }

    private fun countVideos(dir: File): Int {
        var total = 0
        dir.listFiles()?.forEach { child ->
            when {
                child.isBrowsable() -> total += countVideos(child)
                child.isFile && !child.isHidden && isVideoFile(null, child.name) -> total++
            }
        }
        return total
    }

    /** Skips hidden folders and the system-owned `Android` tree so child count stays meaningful. */
    private fun File.isBrowsable(): Boolean = isDirectory && !isHidden && !name.equals("Android", true)

    private fun File.toVideo() = Video(
        uri = Uri.fromFile(this).toString(),
        displayName = name,
        sizeBytes = length(),
        lastModified = lastModified(),
    )
}
