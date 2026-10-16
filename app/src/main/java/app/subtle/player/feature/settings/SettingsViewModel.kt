package app.subtle.player.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.subtle.player.domain.model.AppSettings
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

    private fun persist(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch { repository.update(transform(repository.settings.first())) }
    }
}
