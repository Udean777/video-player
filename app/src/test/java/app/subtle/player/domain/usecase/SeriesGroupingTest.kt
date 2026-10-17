package app.subtle.player.domain.usecase

import app.subtle.player.domain.model.Video
import org.junit.Assert.assertEquals
import org.junit.Test

class SeriesGroupingTest {

    private fun video(name: String) = Video("file:///x/$name", name, 0L, 0L)

    @Test
    fun groupsSeasonEpisodeNamesAndOrdersByEpisode() {
        val groups = SeriesGrouping.group(
            listOf(
                video("Show.S01E03.mkv"),
                video("Show.S01E01.mkv"),
                video("Show.S01E02.mkv"),
            ),
        )

        assertEquals(1, groups.size)
        assertEquals("Show", groups[0].title)
        assertEquals(listOf("Show.S01E01.mkv", "Show.S01E02.mkv", "Show.S01E03.mkv"), groups[0].videos.map { it.displayName })
    }

    @Test
    fun supportsAlternateEpisodeNotation() {
        val groups = SeriesGrouping.group(listOf(video("Show 1x02.mkv"), video("Show 1x01.mkv")))

        assertEquals(1, groups.size)
        assertEquals(listOf("Show 1x01.mkv", "Show 1x02.mkv"), groups[0].videos.map { it.displayName })
    }

    @Test
    fun unmarkedVideosBecomeTheirOwnGroups() {
        val groups = SeriesGrouping.group(listOf(video("holiday.mp4"), video("Show.S02E01.mkv")))

        assertEquals(2, groups.size)
        assertEquals(listOf("Show", "holiday.mp4"), groups.map { it.title }.sortedBy { it })
    }
}
