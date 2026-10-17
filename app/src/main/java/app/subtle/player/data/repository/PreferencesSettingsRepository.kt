package app.subtle.player.data.repository

import android.content.Context
import androidx.core.content.edit
import app.subtle.player.domain.model.AppLanguage
import app.subtle.player.domain.model.AppSettings
import app.subtle.player.domain.model.SubtitleBackground
import app.subtle.player.domain.model.SubtitleColor
import app.subtle.player.domain.model.SubtitleLanguage
import app.subtle.player.domain.model.SubtitleSize
import app.subtle.player.domain.model.SubtitleStyle
import app.subtle.player.domain.model.ThemeMode
import app.subtle.player.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * ponytail: SharedPreferences, not DataStore (no extra dependency). Swap when more structured
 * settings or migrations are needed.
 */
@Singleton
class PreferencesSettingsRepository @Inject constructor(
    @ApplicationContext context: Context,
) : SettingsRepository {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val _settings = MutableStateFlow(read())
    override val settings: Flow<AppSettings> = _settings

    override suspend fun update(settings: AppSettings) {
        prefs.edit {
            putString(KEY_THEME, settings.theme.name)
            putBoolean(KEY_DYNAMIC_COLOR, settings.dynamicColor)
            putBoolean(KEY_FORCE_SOFTWARE, settings.forceSoftwareDecoding)
            putString(KEY_SUBTITLE_SIZE, settings.subtitleStyle.size.name)
            putString(KEY_SUBTITLE_COLOR, settings.subtitleStyle.color.name)
            putBoolean(KEY_SUBTITLE_OUTLINE, settings.subtitleStyle.outline)
            putString(KEY_SUBTITLE_BACKGROUND, settings.subtitleStyle.background.name)
            putBoolean(KEY_SUBTITLE_ASS, settings.subtitleStyle.assUseMyStyle)
            putString(KEY_SUBTITLE_LANGUAGE, settings.subtitleLanguage.name)
            putBoolean(KEY_AUTO_PLAY_NEXT, settings.autoPlayNext)
            putString(KEY_LANGUAGE, settings.language.name)
        }
        _settings.value = settings
    }

    private fun read(): AppSettings = AppSettings(
        theme = ThemeMode.entries.firstOrNull { it.name == prefs.getString(KEY_THEME, null) }
            ?: ThemeMode.System,
        dynamicColor = prefs.getBoolean(KEY_DYNAMIC_COLOR, true),
        forceSoftwareDecoding = prefs.getBoolean(KEY_FORCE_SOFTWARE, false),
        subtitleStyle = SubtitleStyle(
            size = SubtitleSize.entries.firstOrNull { it.name == prefs.getString(KEY_SUBTITLE_SIZE, null) }
                ?: SubtitleSize.Normal,
            color = SubtitleColor.entries.firstOrNull { it.name == prefs.getString(KEY_SUBTITLE_COLOR, null) }
                ?: SubtitleColor.White,
            outline = prefs.getBoolean(KEY_SUBTITLE_OUTLINE, true),
            background = SubtitleBackground.entries
                .firstOrNull { it.name == prefs.getString(KEY_SUBTITLE_BACKGROUND, null) }
                ?: SubtitleBackground.None,
            assUseMyStyle = prefs.getBoolean(KEY_SUBTITLE_ASS, false),
        ),
        subtitleLanguage = SubtitleLanguage.entries
            .firstOrNull { it.name == prefs.getString(KEY_SUBTITLE_LANGUAGE, null) }
            ?: SubtitleLanguage.None,
        autoPlayNext = prefs.getBoolean(KEY_AUTO_PLAY_NEXT, true),
        language = AppLanguage.entries.firstOrNull { it.name == prefs.getString(KEY_LANGUAGE, null) }
            ?: AppLanguage.System,
    )

    private companion object {
        const val PREFS_NAME = "settings"
        const val KEY_THEME = "theme"
        const val KEY_DYNAMIC_COLOR = "dynamic_color"
        const val KEY_FORCE_SOFTWARE = "force_software_decoding"
        const val KEY_SUBTITLE_SIZE = "subtitle_size"
        const val KEY_SUBTITLE_COLOR = "subtitle_color"
        const val KEY_SUBTITLE_OUTLINE = "subtitle_outline"
        const val KEY_SUBTITLE_BACKGROUND = "subtitle_background"
        const val KEY_SUBTITLE_ASS = "subtitle_ass_style"
        const val KEY_SUBTITLE_LANGUAGE = "subtitle_language"
        const val KEY_AUTO_PLAY_NEXT = "auto_play_next"
        const val KEY_LANGUAGE = "language"
    }
}
