package app.subtle.player.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.subtle.player.domain.model.AppLanguage
import app.subtle.player.domain.model.AppSettings
import app.subtle.player.domain.model.SubtitleBackground
import app.subtle.player.domain.model.SubtitleColor
import app.subtle.player.domain.model.SubtitleLanguage
import app.subtle.player.domain.model.SubtitleSize
import app.subtle.player.domain.model.ThemeMode
import app.subtle.player.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: SettingsRepository,
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = repository.settings
        .map { SettingsUiState(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun setTheme(theme: ThemeMode) = persist { it.copy(theme = theme) }

    fun setDynamicColor(enabled: Boolean) = persist { it.copy(dynamicColor = enabled) }

    fun setForceSoftwareDecoding(enabled: Boolean) = persist { it.copy(forceSoftwareDecoding = enabled) }

    fun setSubtitleSize(size: SubtitleSize) = persist { it.copy(subtitleStyle = it.subtitleStyle.copy(size = size)) }

    fun setSubtitleColor(color: SubtitleColor) =
        persist { it.copy(subtitleStyle = it.subtitleStyle.copy(color = color)) }

    fun setSubtitleOutline(enabled: Boolean) =
        persist { it.copy(subtitleStyle = it.subtitleStyle.copy(outline = enabled)) }

    fun setSubtitleBackground(background: SubtitleBackground) =
        persist { it.copy(subtitleStyle = it.subtitleStyle.copy(background = background)) }

    fun setSubtitleLanguage(language: SubtitleLanguage) = persist { it.copy(subtitleLanguage = language) }

    fun setSubtitleAssOverride(enabled: Boolean) =
        persist { it.copy(subtitleStyle = it.subtitleStyle.copy(assUseMyStyle = enabled)) }

    fun setAutoPlayNext(enabled: Boolean) = persist { it.copy(autoPlayNext = enabled) }

    fun setLanguage(language: AppLanguage) = persist { it.copy(language = language) }

    private fun persist(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch { repository.update(transform(repository.settings.first())) }
    }
}
