package app.subtle.player.domain.model

/** Live counters emitted while the library is being scanned. */
data class ScanProgress(
    val foldersScanned: Int,
    val videosFound: Int,
)
