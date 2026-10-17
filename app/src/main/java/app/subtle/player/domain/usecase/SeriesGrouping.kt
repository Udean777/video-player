package app.subtle.player.domain.usecase

import app.subtle.player.domain.model.Video

/** A set of videos that belong together by name, with the videos ordered by episode. */
data class SeriesGroup(
    val title: String,
    val videos: List<Video>,
)

/**
 * Groups videos by a season/episode marker in the file name (`S01E02`, `1x02`). Videos without a
 * marker each become their own single-item group, so nothing is lost. Pure Kotlin, unit-tested.
 */
object SeriesGrouping {

    private val SEASON_EPISODE = Regex("""(?i)^(.*?)[\s._-]*s(\d{1,2})[\s._-]*e(\d{1,3})""")
    private val ALT_EPISODE = Regex("""(?i)^(.*?)[\s._-]*(\d{1,2})x(\d{2,3})""")

    fun group(videos: List<Video>): List<SeriesGroup> {
        val groups = LinkedHashMap<String, MutableList<Pair<Int, Video>>>()
        videos.forEach { video ->
            val parsed = parse(video.displayName)
            val title = parsed?.first ?: video.displayName
            groups.getOrPut(title) { mutableListOf() }.add((parsed?.second ?: 0) to video)
        }
        return groups.map { (title, entries) ->
            SeriesGroup(title, entries.sortedBy { it.first }.map { it.second })
        }.sortedBy { it.title.lowercase() }
    }

    /** Returns the series title and an orderable episode key, or null when the name has no marker. */
    private fun parse(displayName: String): Pair<String, Int>? {
        val match = SEASON_EPISODE.find(displayName) ?: ALT_EPISODE.find(displayName) ?: return null
        val title = match.groupValues[1].trim(' ', '.', '_', '-').ifBlank { return null }
        val season = match.groupValues[2].toIntOrNull() ?: 0
        val episode = match.groupValues[3].toIntOrNull() ?: 0
        return title to (season * 1000 + episode)
    }
}
