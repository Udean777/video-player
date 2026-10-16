package app.subtle.player.data.repository

import android.content.Context
import androidx.core.content.edit
import app.subtle.player.domain.model.AppSettings
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
        }
        _settings.value = settings
    }

    private fun read(): AppSettings = AppSettings(
        theme = ThemeMode.entries.firstOrNull { it.name == prefs.getString(KEY_THEME, null) }
            ?: ThemeMode.System,
        dynamicColor = prefs.getBoolean(KEY_DYNAMIC_COLOR, true),
        forceSoftwareDecoding = prefs.getBoolean(KEY_FORCE_SOFTWARE, false),
    )

    private companion object {
        const val PREFS_NAME = "settings"
        const val KEY_THEME = "theme"
        const val KEY_DYNAMIC_COLOR = "dynamic_color"
        const val KEY_FORCE_SOFTWARE = "force_software_decoding"
    }
}
