package app.subtle.player.domain.model

/** An external subtitle file discovered next to a video. */
data class SubtitleFile(
    val uri: String,
    val displayName: String,
    val languageHint: String?,
)

private val SUBTITLE_EXTENSIONS = setOf("srt", "ass", "ssa", "vtt", "sub", "idx", "smi", "txt")

fun isSubtitleFile(displayName: String): Boolean =
    displayName.substringAfterLast('.', "").lowercase() in SUBTITLE_EXTENSIONS
