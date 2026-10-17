package app.subtle.player.domain.repository

import app.subtle.player.domain.model.DirectoryContent
import app.subtle.player.domain.model.ScanProgress
import app.subtle.player.domain.model.SubtitleFile
import app.subtle.player.domain.model.Video

interface StorageRepository {

    /**
     * Every video under the storage root, recursively. [onProgress] is called as the walk advances
     * so the UI can show progress; the scan is cancellable through the caller's coroutine.
     */
    suspend fun allVideos(onProgress: (ScanProgress) -> Unit = {}): List<Video>

    /** Contents of [path]; null means the storage root. Only folders containing videos are listed. */
    suspend fun listDirectory(path: String?): DirectoryContent

    /** External subtitle files sitting next to [videoUri] and matching its name. Empty for non-file URIs. */
    suspend fun subtitlesFor(videoUri: String): List<SubtitleFile>
}
