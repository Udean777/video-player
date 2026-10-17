package app.subtle.player.domain.usecase

import app.subtle.player.domain.model.PlaybackProgress
import app.subtle.player.domain.model.Video
import org.junit.Assert.assertEquals
import org.junit.Test

class FilterAndSortVideosTest {

    @Test
    fun filtersByNameCaseInsensitive() {
        val videos = listOf(video("a", "Harbor.Lights.mp4", modified = 3L), video("b", "night.mp4", modified = 1L))

        val result = filterAndSortVideos(videos, query = "harbor")

        assertEquals(listOf("Harbor.Lights.mp4"), result.map { it.displayName })
    }

    @Test
    fun blankQueryKeepsEverything() {
        val videos = listOf(video("a", "b.mp4", 1L), video("b", "a.mp4", 2L))

        assertEquals(videos.size, filterAndSortVideos(videos, query = "  ").size)
    }

    @Test
    fun sortsByName() {
        val videos = listOf(video("z", "Zeta.mp4", 1L), video("a", "alpha.mp4", 2L))

        assertEquals(listOf("alpha.mp4", "Zeta.mp4"), filterAndSortVideos(videos, sort = VideoSort.Name).map { it.displayName })
    }

    @Test
    fun sortsByDateAddedNewestFirst() {
        val videos = listOf(video("old", "old.mp4", 1L), video("new", "new.mp4", 9L))

        assertEquals(listOf("new.mp4", "old.mp4"), filterAndSortVideos(videos, sort = VideoSort.DateAdded).map { it.displayName })
    }

    @Test
    fun sortsByLastPlayedWithNeverPlayedLast() {
        val videos = listOf(video("a", "a.mp4", 1L), video("b", "b.mp4", 1L), video("c", "c.mp4", 1L))
        val progress = mapOf(
            "a" to PlaybackProgress(0, 1, lastPlayedMs = 100L),
            "c" to PlaybackProgress(0, 1, lastPlayedMs = 300L),
        )

        val result = filterAndSortVideos(videos, sort = VideoSort.LastPlayed, progress = progress)

        assertEquals(listOf("c.mp4", "a.mp4", "b.mp4"), result.map { it.displayName })
    }

    private fun video(uri: String, name: String, modified: Long) =
        Video(uri = uri, displayName = name, sizeBytes = 0L, lastModified = modified)
}
