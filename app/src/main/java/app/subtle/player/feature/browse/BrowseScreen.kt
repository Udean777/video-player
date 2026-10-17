package app.subtle.player.feature.browse

import android.content.res.Configuration
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.subtle.player.R
import app.subtle.player.core.designsystem.SubtleTheme
import app.subtle.player.core.designsystem.components.EmptyState
import app.subtle.player.core.designsystem.rememberHeaderBrush
import app.subtle.player.domain.model.Breadcrumb
import app.subtle.player.domain.model.FolderEntry
import app.subtle.player.domain.model.Video
import app.subtle.player.feature.common.BrowserActions
import app.subtle.player.feature.common.BrowserViewMode
import app.subtle.player.feature.common.FolderCell
import app.subtle.player.feature.common.FolderRow
import app.subtle.player.feature.common.VideoCell
import app.subtle.player.feature.common.VideoRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowseScreen(
    state: BrowseUiState,
    onFolderClick: (FolderEntry) -> Unit,
    onBreadcrumbClick: (Int) -> Unit,
    onUp: () -> Unit,
    onRefresh: () -> Unit,
    onViewModeChange: (BrowserViewMode) -> Unit,
    onVideoClick: (Video) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            Column(Modifier.fillMaxWidth().background(rememberHeaderBrush())) {
                TopAppBar(
                    title = { Text(state.path.lastOrNull()?.name ?: stringResource(R.string.tab_browse)) },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                    navigationIcon = {
                        if (state.canGoUp) {
                            IconButton(onClick = onUp) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                            }
                        }
                    },
                    actions = {
                        BrowserActions(
                            mode = state.viewMode,
                            onViewModeChange = onViewModeChange,
                            onRefresh = onRefresh,
                        )
                    },
                )
                BreadcrumbRow(path = state.path, onBreadcrumbClick = onBreadcrumbClick)
            }
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                state.isLoading && state.folders.isEmpty() && state.videos.isEmpty() -> {
                    CircularProgressIndicator(Modifier.align(Alignment.Center))
                }

                state.folders.isEmpty() && state.videos.isEmpty() -> {
                    EmptyState(icon = Icons.Filled.FolderOpen, title = stringResource(R.string.no_entries))
                }

                else -> Crossfade(targetState = state.viewMode, label = "viewMode") { mode ->
                    if (mode == BrowserViewMode.List) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(vertical = 8.dp),
                        ) {
                            items(state.folders, key = { "f:${it.path}" }) { folder ->
                                FolderRow(folder = folder, onClick = { onFolderClick(folder) })
                            }
                            items(state.videos, key = { "v:${it.uri}" }) { video ->
                                VideoRow(
                                    video = video,
                                    progressFraction = state.progress[video.uri]?.fraction,
                                    onClick = { onVideoClick(video) },
                                )
                            }
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(160.dp),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(state.folders, key = { "f:${it.path}" }) { folder ->
                                FolderCell(folder = folder, onClick = { onFolderClick(folder) })
                            }
                            items(state.videos, key = { "v:${it.uri}" }) { video ->
                                VideoCell(
                                    video = video,
                                    progressFraction = state.progress[video.uri]?.fraction,
                                    onClick = { onVideoClick(video) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BreadcrumbRow(path: List<Breadcrumb>, onBreadcrumbClick: (Int) -> Unit) {
    if (path.isEmpty()) return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        path.forEachIndexed { index, crumb ->
            if (index > 0) {
                Icon(
                    Icons.Filled.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp),
                )
            }
            AssistChip(
                onClick = { onBreadcrumbClick(index) },
                label = { Text(crumb.name, maxLines = 1) },
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun BrowseListPreview() {
    SubtleTheme(darkTheme = false) {
        BrowseScreen(
            state = BrowseUiState(
                path = listOf(Breadcrumb("Internal storage", null)),
                folders = listOf(FolderEntry("Movies", "/videos/Movies", 4)),
                videos = listOf(Video("file:///videos/a.mp4", "harbor.lights.mp4", 800_000_000L, 0L)),
                viewMode = BrowserViewMode.List,
            ),
            onFolderClick = {},
            onBreadcrumbClick = {},
            onUp = {},
            onRefresh = {},
            onViewModeChange = {},
            onVideoClick = {},
        )
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun BrowseGridPreview() {
    SubtleTheme(darkTheme = true) {
        BrowseScreen(
            state = BrowseUiState(
                path = listOf(Breadcrumb("Internal storage", null), Breadcrumb("Action", "/videos/Movies/Action")),
                folders = listOf(FolderEntry("Classics", "/videos/Movies/Action/Classics", 12)),
                videos = listOf(
                    Video("file:///videos/a.mkv", "night.chase.2024.mkv", 2_500_000_000L, 0L),
                    Video("file:///videos/b.mp4", "harbor.lights.mp4", 800_000_000L, 0L),
                ),
                viewMode = BrowserViewMode.Grid,
            ),
            onFolderClick = {},
            onBreadcrumbClick = {},
            onUp = {},
            onRefresh = {},
            onViewModeChange = {},
            onVideoClick = {},
        )
    }
}
