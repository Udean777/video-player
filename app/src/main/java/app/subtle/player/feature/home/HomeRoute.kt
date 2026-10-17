package app.subtle.player.feature.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun HomeRoute(
    onVideoClick: (String) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    HomeScreen(
        state = uiState,
        onViewModeChange = viewModel::setViewMode,
        onRefresh = viewModel::refresh,
        onCancelScan = viewModel::cancelScan,
        onVideoClick = { onVideoClick(it.uri) },
        onOpenSettings = onOpenSettings,
        onQueryChange = viewModel::setQuery,
        onSortChange = viewModel::setSort,
        onGroupSeriesChange = viewModel::setGroupSeries,
        modifier = modifier,
    )
}
