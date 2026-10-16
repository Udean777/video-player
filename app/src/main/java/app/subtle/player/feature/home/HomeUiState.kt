package app.subtle.player.feature.home

import app.subtle.player.domain.model.PlaybackProgress
import app.subtle.player.domain.model.Video
import app.subtle.player.feature.common.BrowserViewMode

data class HomeUiState(
    val videos: List<Video> = emptyList(),
    val progress: Map<String, PlaybackProgress> = emptyMap(),
    val isLoading: Boolean = false,
    val viewMode: BrowserViewMode = BrowserViewMode.List,
)
