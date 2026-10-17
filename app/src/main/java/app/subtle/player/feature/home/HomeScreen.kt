package app.subtle.player.feature.home

import android.content.res.Configuration
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.subtle.player.R
import app.subtle.player.core.designsystem.SubtleTheme
import app.subtle.player.domain.model.PlaybackProgress
import app.subtle.player.domain.model.Video
import app.subtle.player.domain.usecase.SeriesGroup
import app.subtle.player.domain.usecase.VideoSort
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
    onCancelScan: () -> Unit,
    onVideoClick: (Video) -> Unit,
    onOpenSettings: () -> Unit,
    onQueryChange: (String) -> Unit,
    onSortChange: (VideoSort) -> Unit,
    onGroupSeriesChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var sortMenuVisible by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val gridState = rememberLazyGridState()

    // The list keeps its scroll index when the filtered set shrinks, which lands at the last item.
    LaunchedEffect(state.query) {
        listState.scrollToItem(0)
        gridState.scrollToItem(0)
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.tab_home)) },
                actions = {
                    IconButton(onClick = { sortMenuVisible = true }) {
                        Icon(Icons.Filled.Sort, stringResource(R.string.sort_by))
                    }
                    SortMenu(
                        visible = sortMenuVisible,
                        selected = state.sort,
                        groupSeries = state.groupSeries,
                        onSelect = {
                            onSortChange(it)
                            sortMenuVisible = false
                        },
                        onToggleGroup = { onGroupSeriesChange(!state.groupSeries) },
                        onDismiss = { sortMenuVisible = false },
                    )
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
        PullToRefreshBox(
            isRefreshing = state.isScanning,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            Column(Modifier.fillMaxSize()) {
                SearchField(query = state.query, onQueryChange = onQueryChange)
                if (state.isScanning) {
                    ScanBanner(
                        scannedFolders = state.scannedFolders,
                        videosFound = state.videosFound,
                        onCancel = onCancelScan,
                    )
                }
                Box(Modifier.fillMaxWidth().weight(1f)) {
                    when {
                        state.isScanning && state.videos.isEmpty() -> {
                            CircularProgressIndicator(Modifier.align(Alignment.Center))
                        }

                        state.videos.isEmpty() -> {
                            Text(stringResource(R.string.no_entries), Modifier.align(Alignment.Center))
                        }

                        state.viewMode == BrowserViewMode.List && state.groupSeries -> {
                            GroupedVideoList(
                                groups = state.groups,
                                progress = state.progress,
                                listState = listState,
                                onVideoClick = onVideoClick,
                            )
                        }

                        state.viewMode == BrowserViewMode.List -> {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(vertical = 8.dp),
                            ) {
                                items(state.videos) { video ->
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
                                state = gridState,
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                items(state.videos) { video ->
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
}

@Composable
private fun ScanBanner(scannedFolders: Int, videosFound: Int, onCancel: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.scan_in_progress, videosFound, scannedFolders),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onCancel) { Text(stringResource(R.string.action_cancel)) }
        }
        LinearProgressIndicator(Modifier.fillMaxWidth())
    }
}

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        placeholder = { Text(stringResource(R.string.search_hint)) },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Filled.Close, stringResource(R.string.clear))
                }
            }
        },
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@Composable
private fun SortMenu(
    visible: Boolean,
    selected: VideoSort,
    groupSeries: Boolean,
    onSelect: (VideoSort) -> Unit,
    onToggleGroup: () -> Unit,
    onDismiss: () -> Unit,
) {
    DropdownMenu(expanded = visible, onDismissRequest = onDismiss) {
        VideoSort.entries.forEach { option ->
            DropdownMenuItem(
                text = { Text(stringResource(option.labelRes())) },
                onClick = { onSelect(option) },
                leadingIcon = if (option == selected) {
                    { Icon(Icons.Filled.Check, contentDescription = null) }
                } else {
                    null
                },
            )
        }
        HorizontalDivider()
        DropdownMenuItem(
            text = { Text(stringResource(R.string.group_series)) },
            onClick = onToggleGroup,
            leadingIcon = if (groupSeries) {
                { Icon(Icons.Filled.Check, contentDescription = null) }
            } else {
                null
            },
        )
    }
}

@Composable
private fun GroupedVideoList(
    groups: List<SeriesGroup>,
    progress: Map<String, PlaybackProgress>,
    listState: LazyListState,
    onVideoClick: (Video) -> Unit,
) {
    var expanded by remember(groups) { mutableStateOf(groups.map { it.title }.toSet()) }
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp),
    ) {
        groups.forEach { group ->
            val isExpanded = group.title in expanded
            item {
                GroupHeader(
                    title = group.title,
                    count = group.videos.size,
                    expanded = isExpanded,
                    onToggle = {
                        expanded = if (isExpanded) expanded - group.title else expanded + group.title
                    },
                )
            }
            if (isExpanded) {
                items(group.videos) { video ->
                    VideoRow(
                        video = video,
                        progressFraction = progress[video.uri]?.fraction,
                        onClick = { onVideoClick(video) },
                    )
                }
            }
        }
    }
}

@Composable
private fun GroupHeader(title: String, count: Int, expanded: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
            contentDescription = null,
        )
        Text(
            text = stringResource(R.string.series_group_count, title, count),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

@StringRes
private fun VideoSort.labelRes(): Int = when (this) {
    VideoSort.Name -> R.string.sort_name
    VideoSort.DateAdded -> R.string.sort_date_added
    VideoSort.LastPlayed -> R.string.sort_last_played
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
            onCancelScan = {},
            onVideoClick = {},
            onOpenSettings = {},
            onQueryChange = {},
            onSortChange = {},
            onGroupSeriesChange = {},
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
            onCancelScan = {},
            onVideoClick = {},
            onOpenSettings = {},
            onQueryChange = {},
            onSortChange = {},
            onGroupSeriesChange = {},
        )
    }
}
