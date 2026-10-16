package app.subtle.player.feature.browse

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun BrowseRoute(
    onVideoClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BrowseViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    BackHandler(enabled = uiState.canGoUp) {
        viewModel.goUp()
    }

    BrowseScreen(
        state = uiState,
        onFolderClick = viewModel::openFolder,
        onBreadcrumbClick = viewModel::openPath,
        onUp = viewModel::goUp,
        onRefresh = viewModel::refresh,
        onViewModeChange = viewModel::setViewMode,
        onVideoClick = { onVideoClick(it.uri) },
        modifier = modifier,
    )
}
