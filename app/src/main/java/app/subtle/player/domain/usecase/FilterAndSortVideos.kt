package app.subtle.player.domain.usecase

import app.subtle.player.domain.model.PlaybackProgress
import app.subtle.player.domain.model.Video

enum class VideoSort { Name, DateAdded, LastPlayed }

/**
 * Filters [videos] by [query] (case-insensitive substring of the file name) and orders them.
 * [progress] only feeds [VideoSort.LastPlayed]; never-played videos sort last.
 */
fun filterAndSortVideos(
    videos: List<Video>,
    query: String = "",
    sort: VideoSort = VideoSort.Name,
    progress: Map<String, PlaybackProgress> = emptyMap(),
): List<Video> {
    val trimmed = query.trim()
    val filtered =
        if (trimmed.isEmpty()) videos
        else videos.filter { it.displayName.contains(trimmed, ignoreCase = true) }

    return when (sort) {
        VideoSort.Name -> filtered.sortedBy { it.displayName.lowercase() }
        VideoSort.DateAdded -> filtered.sortedByDescending { it.lastModified }
        VideoSort.LastPlayed -> filtered.sortedWith(
            compareByDescending<Video> { progress[it.uri]?.lastPlayedMs ?: 0L }
                .thenBy { it.displayName.lowercase() },
        )
    }
}
