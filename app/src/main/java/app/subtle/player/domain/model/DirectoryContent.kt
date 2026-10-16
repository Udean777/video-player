package app.subtle.player.domain.model

/** A folder in the browse path. [path] is null for the storage root. */
data class Breadcrumb(val name: String, val path: String?)

data class FolderEntry(
    val name: String,
    val path: String,
    /** Videos in this folder and all its subfolders. Folders with zero are never listed. */
    val videoCount: Int,
)

data class DirectoryContent(
    val displayName: String,
    val folders: List<FolderEntry>,
    val videos: List<Video>,
) {
    val isEmpty: Boolean
        get() = folders.isEmpty() && videos.isEmpty()
}
