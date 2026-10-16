package app.subtle.player.feature.browse

import app.subtle.player.domain.model.Breadcrumb
import app.subtle.player.domain.model.FolderEntry
import app.subtle.player.domain.model.PlaybackProgress
import app.subtle.player.domain.model.Video
import app.subtle.player.feature.common.BrowserViewMode

data class BrowseUiState(
    val path: List<Breadcrumb> = emptyList(),
    val folders: List<FolderEntry> = emptyList(),
    val videos: List<Video> = emptyList(),
    val progress: Map<String, PlaybackProgress> = emptyMap(),
    val isLoading: Boolean = false,
    val viewMode: BrowserViewMode = BrowserViewMode.List,
) {
    val canGoUp: Boolean
        get() = path.size > 1
}
