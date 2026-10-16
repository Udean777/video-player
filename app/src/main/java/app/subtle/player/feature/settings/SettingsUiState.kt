package app.subtle.player.feature.settings

import app.subtle.player.domain.model.AppSettings

data class SettingsUiState(
    val settings: AppSettings = AppSettings(),
)
