package app.subtle.player.feature.settings

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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
import app.subtle.player.domain.model.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onThemeChange: (ThemeMode) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    onForceSoftwareDecodingChange: (Boolean) -> Unit,
    onOpenAbout: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            SectionHeader(stringResource(R.string.settings_theme))
            ThemeRow(R.string.settings_theme_system, ThemeMode.System, state.settings.theme, onThemeChange)
            ThemeRow(R.string.settings_theme_light, ThemeMode.Light, state.settings.theme, onThemeChange)
            ThemeRow(R.string.settings_theme_dark, ThemeMode.Dark, state.settings.theme, onThemeChange)

            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            SwitchRow(
                title = stringResource(R.string.settings_dynamic_color),
                subtitle = stringResource(R.string.settings_dynamic_color_desc),
                checked = state.settings.dynamicColor,
                onCheckedChange = onDynamicColorChange,
            )
            SwitchRow(
                title = stringResource(R.string.settings_force_software),
                subtitle = stringResource(R.string.settings_force_software_desc),
                checked = state.settings.forceSoftwareDecoding,
                onCheckedChange = onForceSoftwareDecodingChange,
            )

            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_about)) },
                supportingContent = { Text(stringResource(R.string.settings_about_desc)) },
                leadingContent = { Icon(Icons.Filled.Info, contentDescription = null) },
                trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) },
                modifier = Modifier.clickable(onClick = onOpenAbout),
            )
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@Composable
private fun ThemeRow(
    titleRes: Int,
    mode: ThemeMode,
    selected: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onThemeChange(mode) }.padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(stringResource(titleRes), modifier = Modifier.weight(1f))
        RadioButton(selected = selected == mode, onClick = { onThemeChange(mode) })
    }
}

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        trailingContent = { Switch(checked = checked, onCheckedChange = onCheckedChange) },
    )
}

@Preview(showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    SubtleTheme(darkTheme = false) {
        SettingsScreen(
            state = SettingsUiState(),
            onThemeChange = {},
            onDynamicColorChange = {},
            onForceSoftwareDecodingChange = {},
            onOpenAbout = {},
            onBack = {},
        )
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SettingsScreenDarkPreview() {
    SubtleTheme(darkTheme = true) {
        SettingsScreen(
            state = SettingsUiState(),
            onThemeChange = {},
            onDynamicColorChange = {},
            onForceSoftwareDecodingChange = {},
            onOpenAbout = {},
            onBack = {},
        )
    }
}
