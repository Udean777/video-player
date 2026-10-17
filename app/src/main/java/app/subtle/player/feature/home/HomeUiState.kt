package app.subtle.player.feature.home

import app.subtle.player.domain.model.PlaybackProgress
import app.subtle.player.domain.model.Video
import app.subtle.player.domain.usecase.SeriesGroup
import app.subtle.player.domain.usecase.VideoSort
import app.subtle.player.feature.common.BrowserViewMode

data class HomeUiState(
    val videos: List<Video> = emptyList(),
    val groups: List<SeriesGroup> = emptyList(),
    val progress: Map<String, PlaybackProgress> = emptyMap(),
    val isScanning: Boolean = false,
    val scannedFolders: Int = 0,
    val videosFound: Int = 0,
    val viewMode: BrowserViewMode = BrowserViewMode.List,
    val query: String = "",
    val sort: VideoSort = VideoSort.Name,
    val groupSeries: Boolean = false,
)
