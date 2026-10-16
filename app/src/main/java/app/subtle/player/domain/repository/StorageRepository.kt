package app.subtle.player.domain.repository

import app.subtle.player.domain.model.DirectoryContent
import app.subtle.player.domain.model.SubtitleFile
import app.subtle.player.domain.model.Video

interface StorageRepository {

    /** Every video under the storage root, recursively. */
    suspend fun allVideos(): List<Video>

    /** Contents of [path]; null means the storage root. Only folders containing videos are listed. */
    suspend fun listDirectory(path: String?): DirectoryContent

    /** External subtitle files sitting next to [videoUri] and matching its name. Empty for non-file URIs. */
    suspend fun subtitlesFor(videoUri: String): List<SubtitleFile>
}
