package app.subtle.player.domain.model

/** URIs are plain strings so `domain` stays free of Android imports. */
data class Video(
    val uri: String,
    val displayName: String,
    val sizeBytes: Long,
    val lastModified: Long,
)

private val VIDEO_EXTENSIONS = setOf(
    "mp4", "mkv", "avi", "mov", "webm", "ts", "m2ts", "mts",
    "flv", "ogv", "ogg", "3gp", "3g2", "wmv", "mpg", "mpeg", "m4v",
)

/**
 * True for video files. Providers sometimes report a generic MIME type (or none),
 * so the file extension is the fallback.
 */
fun isVideoFile(mimeType: String?, displayName: String): Boolean {
    if (mimeType != null && mimeType.startsWith("video/")) return true
    return displayName.substringAfterLast('.', "").lowercase() in VIDEO_EXTENSIONS
}
