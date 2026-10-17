package app.subtle.player.feature.settings

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.subtle.player.R
import app.subtle.player.core.designsystem.SubtleTheme
import app.subtle.player.core.designsystem.rememberHeaderBrush
import app.subtle.player.domain.model.AppLanguage
import app.subtle.player.domain.model.SubtitleBackground
import app.subtle.player.domain.model.SubtitleColor
import app.subtle.player.domain.model.SubtitleLanguage
import app.subtle.player.domain.model.SubtitleSize
import app.subtle.player.domain.model.SubtitleStyle
import app.subtle.player.domain.model.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onThemeChange: (ThemeMode) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    onForceSoftwareDecodingChange: (Boolean) -> Unit,
    onSubtitleSizeChange: (SubtitleSize) -> Unit,
    onSubtitleColorChange: (SubtitleColor) -> Unit,
    onSubtitleOutlineChange: (Boolean) -> Unit,
    onSubtitleBackgroundChange: (SubtitleBackground) -> Unit,
    onSubtitleAssOverrideChange: (Boolean) -> Unit,
    onSubtitleLanguageChange: (SubtitleLanguage) -> Unit,
    onAutoPlayNextChange: (Boolean) -> Unit,
    onLanguageChange: (AppLanguage) -> Unit,
    onOpenAbout: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            Box(Modifier.fillMaxWidth().background(rememberHeaderBrush())) {
                TopAppBar(
                    title = { Text(stringResource(R.string.settings_title)) },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                        }
                    },
                )
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            SettingsCard {
                SectionHeader(stringResource(R.string.settings_theme))
                OptionRow(R.string.settings_theme_system, ThemeMode.System, state.settings.theme, onThemeChange)
                OptionRow(R.string.settings_theme_light, ThemeMode.Light, state.settings.theme, onThemeChange)
                OptionRow(R.string.settings_theme_dark, ThemeMode.Dark, state.settings.theme, onThemeChange)
            }
            CardGap()
            SettingsCard {
                Label(stringResource(R.string.settings_language))
                AppLanguage.entries.forEach {
                    OptionRow(it.labelRes(), it, state.settings.language, onLanguageChange)
                }
            }
            CardGap()
            SettingsCard {
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
                SwitchRow(
                    title = stringResource(R.string.settings_auto_play_next),
                    subtitle = stringResource(R.string.settings_auto_play_next_desc),
                    checked = state.settings.autoPlayNext,
                    onCheckedChange = onAutoPlayNextChange,
                )
            }
            CardGap()
            SettingsCard {
                SectionHeader(stringResource(R.string.settings_subtitle_style))
                Text(
                    text = stringResource(R.string.settings_subtitle_style_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
                SubtitleStylePreview(
                    style = state.settings.subtitleStyle,
                    modifier = Modifier.padding(16.dp),
                )
                Label(stringResource(R.string.subtitle_style_size))
                SubtitleSize.entries.forEach {
                    OptionRow(it.labelRes(), it, state.settings.subtitleStyle.size, onSubtitleSizeChange)
                }
                Label(stringResource(R.string.subtitle_style_color))
                SubtitleColor.entries.forEach {
                    OptionRow(it.labelRes(), it, state.settings.subtitleStyle.color, onSubtitleColorChange)
                }
                Label(stringResource(R.string.subtitle_style_background))
                SubtitleBackground.entries.forEach {
                    OptionRow(it.labelRes(), it, state.settings.subtitleStyle.background, onSubtitleBackgroundChange)
                }
                SwitchRow(
                    title = stringResource(R.string.subtitle_style_outline),
                    subtitle = stringResource(R.string.subtitle_style_outline_desc),
                    checked = state.settings.subtitleStyle.outline,
                    onCheckedChange = onSubtitleOutlineChange,
                )
                SwitchRow(
                    title = stringResource(R.string.subtitle_style_ass),
                    subtitle = stringResource(R.string.subtitle_style_ass_desc),
                    checked = state.settings.subtitleStyle.assUseMyStyle,
                    onCheckedChange = onSubtitleAssOverrideChange,
                )
            }
            CardGap()
            SettingsCard {
                Label(stringResource(R.string.settings_subtitle_language))
                Text(
                    text = stringResource(R.string.settings_subtitle_language_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
                SubtitleLanguage.entries.forEach {
                    OptionRow(it.labelRes(), it, state.settings.subtitleLanguage, onSubtitleLanguageChange)
                }
            }
            CardGap()
            SettingsCard {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_about)) },
                    supportingContent = { Text(stringResource(R.string.settings_about_desc)) },
                    leadingContent = { Icon(Icons.Filled.Info, contentDescription = null) },
                    trailingContent = {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
                    },
                    modifier = Modifier.clickable(onClick = onOpenAbout),
                )
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(Modifier.fillMaxWidth().padding(vertical = 4.dp), content = content)
    }
}

@Composable
private fun CardGap() {
    Spacer(Modifier.height(12.dp))
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
private fun Label(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp),
    )
}

@Composable
private fun <T> OptionRow(
    titleRes: Int,
    value: T,
    selected: T,
    onSelect: (T) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onSelect(value) }.padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(stringResource(titleRes), modifier = Modifier.weight(1f))
        RadioButton(selected = selected == value, onClick = { onSelect(value) })
    }
}

@Composable
private fun SubtitleStylePreview(style: SubtitleStyle, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(160.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(Color.Black),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Text(
            text = stringResource(R.string.subtitle_style_preview_text),
            color = style.color.previewColor,
            fontSize = (16 * style.size.previewScale).sp,
            textAlign = TextAlign.Center,
            style = TextStyle(
                shadow = if (style.outline) Shadow(color = Color.Black, blurRadius = 3f) else null,
            ),
            modifier = Modifier
                .padding(bottom = 16.dp, start = 12.dp, end = 12.dp)
                .background(
                    color = Color.Black.copy(alpha = style.background.previewAlpha),
                    shape = RoundedCornerShape(4.dp),
                )
                .padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}

private val SubtitleSize.previewScale: Float
    get() = when (this) {
        SubtitleSize.Small -> 0.75f
        SubtitleSize.Normal -> 1f
        SubtitleSize.Big -> 1.35f
        SubtitleSize.Huge -> 1.75f
    }

private val SubtitleColor.previewColor: Color
    get() = Color(0xFF000000L or rgb.toLong())

private val SubtitleBackground.previewAlpha: Float
    get() = when (this) {
        SubtitleBackground.None -> 0f
        SubtitleBackground.Subtle -> 0.5f
        SubtitleBackground.Solid -> 0.9f
    }

private fun SubtitleSize.labelRes(): Int = when (this) {
    SubtitleSize.Small -> R.string.subtitle_size_small
    SubtitleSize.Normal -> R.string.subtitle_size_normal
    SubtitleSize.Big -> R.string.subtitle_size_big
    SubtitleSize.Huge -> R.string.subtitle_size_huge
}

private fun SubtitleColor.labelRes(): Int = when (this) {
    SubtitleColor.White -> R.string.subtitle_color_white
    SubtitleColor.Yellow -> R.string.subtitle_color_yellow
    SubtitleColor.Cyan -> R.string.subtitle_color_cyan
}

private fun SubtitleBackground.labelRes(): Int = when (this) {
    SubtitleBackground.None -> R.string.subtitle_bg_none
    SubtitleBackground.Subtle -> R.string.subtitle_bg_subtle
    SubtitleBackground.Solid -> R.string.subtitle_bg_solid
}

private fun SubtitleLanguage.labelRes(): Int = when (this) {
    SubtitleLanguage.None -> R.string.subtitle_language_none
    SubtitleLanguage.English -> R.string.subtitle_language_english
    SubtitleLanguage.Indonesian -> R.string.subtitle_language_indonesian
}

private fun AppLanguage.labelRes(): Int = when (this) {
    AppLanguage.System -> R.string.settings_language_system
    AppLanguage.English -> R.string.settings_language_english
    AppLanguage.Indonesian -> R.string.settings_language_indonesian
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
            onSubtitleSizeChange = {},
            onSubtitleColorChange = {},
            onSubtitleOutlineChange = {},
            onSubtitleBackgroundChange = {},
            onSubtitleAssOverrideChange = {},
            onSubtitleLanguageChange = {},
            onAutoPlayNextChange = {},
            onLanguageChange = {},
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
            onSubtitleSizeChange = {},
            onSubtitleColorChange = {},
            onSubtitleOutlineChange = {},
            onSubtitleBackgroundChange = {},
            onSubtitleAssOverrideChange = {},
            onSubtitleLanguageChange = {},
            onAutoPlayNextChange = {},
            onLanguageChange = {},
            onOpenAbout = {},
            onBack = {},
        )
    }
}
