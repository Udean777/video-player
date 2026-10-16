package app.subtle.player.feature.home

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.subtle.player.R
import app.subtle.player.core.designsystem.SubtleTheme
import app.subtle.player.domain.model.Video
import app.subtle.player.feature.common.BrowserActions
import app.subtle.player.feature.common.BrowserViewMode
import app.subtle.player.feature.common.VideoCell
import app.subtle.player.feature.common.VideoRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: HomeUiState,
    onViewModeChange: (BrowserViewMode) -> Unit,
    onRefresh: () -> Unit,
    onVideoClick: (Video) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.tab_home)) },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, stringResource(R.string.settings_title))
                    }
                    BrowserActions(
                        mode = state.viewMode,
                        onViewModeChange = onViewModeChange,
                        onRefresh = onRefresh,
                    )
                },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                state.isLoading && state.videos.isEmpty() -> {
                    CircularProgressIndicator(Modifier.align(Alignment.Center))
                }

                state.videos.isEmpty() -> {
                    Text(stringResource(R.string.no_entries), Modifier.align(Alignment.Center))
                }

                state.viewMode == BrowserViewMode.List -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 8.dp),
                    ) {
                        items(state.videos, key = { it.uri }) { video ->
                            VideoRow(
                                video = video,
                                progressFraction = state.progress[video.uri]?.fraction,
                                onClick = { onVideoClick(video) },
                            )
                        }
                    }
                }

                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(160.dp),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(state.videos, key = { it.uri }) { video ->
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

@Preview(showBackground = true)
@Composable
private fun HomePreview() {
    SubtleTheme(darkTheme = false) {
        HomeScreen(
            state = HomeUiState(
                videos = listOf(
                    Video("file:///videos/a.mp4", "harbor.lights.mp4", 800_000_000L, 0L),
                    Video("file:///videos/b.mkv", "night.chase.2024.mkv", 2_500_000_000L, 0L),
                ),
            ),
            onViewModeChange = {},
            onRefresh = {},
            onVideoClick = {},
            onOpenSettings = {},
        )
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun HomeGridPreview() {
    SubtleTheme(darkTheme = true) {
        HomeScreen(
            state = HomeUiState(
                videos = listOf(
                    Video("file:///videos/a.mp4", "harbor.lights.mp4", 800_000_000L, 0L),
                    Video("file:///videos/b.mkv", "night.chase.2024.mkv", 2_500_000_000L, 0L),
                ),
                viewMode = BrowserViewMode.Grid,
            ),
            onViewModeChange = {},
            onRefresh = {},
            onVideoClick = {},
            onOpenSettings = {},
        )
    }
}
