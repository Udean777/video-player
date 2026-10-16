package app.subtle.player.feature.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun SettingsRoute(
    onOpenAbout: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    SettingsScreen(
        state = uiState,
        onThemeChange = viewModel::setTheme,
        onDynamicColorChange = viewModel::setDynamicColor,
        onForceSoftwareDecodingChange = viewModel::setForceSoftwareDecoding,
        onOpenAbout = onOpenAbout,
        onBack = onBack,
        modifier = modifier,
    )
}
