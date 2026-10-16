package app.subtle.player.domain.usecase

/**
 * Matches external subtitle file names to a video by base name. Pure Kotlin, unit-tested.
 *
 * A subtitle matches when its name equals the video base name (ignoring case) or extends it with
 * a suffix: `Movie.mkv` matches `Movie.srt` and `Movie.en.srt`, but not `Movie 2.srt`.
 */
object SubtitleMatcher {

    fun matches(videoName: String, subtitleName: String): Boolean {
        val video = videoName.substringBeforeLast('.')
        val subtitle = subtitleName.substringBeforeLast('.')
        return subtitle.equals(video, ignoreCase = true) ||
            subtitle.startsWith("$video.", ignoreCase = true)
    }

    /** `Movie.en.srt` for `Movie.mkv` -> `en`; an exact base name -> null. */
    fun languageHint(videoName: String, subtitleName: String): String? {
        val video = videoName.substringBeforeLast('.')
        val subtitle = subtitleName.substringBeforeLast('.')
        return subtitle.removePrefix(video).removePrefix(".").takeIf { it.isNotBlank() }
    }
}
