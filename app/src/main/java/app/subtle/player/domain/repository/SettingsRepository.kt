package app.subtle.player.domain.repository

import app.subtle.player.domain.model.AppSettings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {

    val settings: Flow<AppSettings>

    suspend fun update(settings: AppSettings)
}
