package app.subtle.player.feature.common

import app.subtle.player.domain.model.AppSettings
import app.subtle.player.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeSettingsRepository(initial: AppSettings = AppSettings()) : SettingsRepository {
    private val _settings = MutableStateFlow(initial)
    val current: AppSettings get() = _settings.value
    override val settings: Flow<AppSettings> = _settings
    override suspend fun update(settings: AppSettings) {
        _settings.value = settings
    }
}
